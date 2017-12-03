package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.ExecutionErrorDialog;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.concurrency.FutureResult;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

public class OpenOcdComponent {

    private static final String ERROR_PREFIX = "Error: ";
    private static final String FLASH_FAIL_TEXT = "** Programming Failed **";
    private static final String FLASH_SUCCESS_TEXT = "** Programming Finished **";
    private static final Logger LOG = Logger.getInstance(OpenOcdRun.class);
    private final EditorColorsScheme myColorsScheme;
    private OSProcessHandler process;

    public OpenOcdComponent() {
        myColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public static GeneralCommandLine createOcdCommandLine(@NotNull Project project, @Nullable File fileToLoad, @Nullable String additionalCommand, boolean shutdown) throws ConfigurationException {
        OpenOcdSettingsState ocdSettings = project.getComponent(OpenOcdSettingsState.class);
        if (ocdSettings.boardConfigFile == null || "".equals(ocdSettings.boardConfigFile.trim())) {
            throw new ConfigurationException("Board Config file is not defined.\nPlease open OpenOCD settings and choose one.", "OpenOCD run error");
        }
        File openOcdBinFolder = new File(ocdSettings.openOcdHome, "bin");
        if (!openOcdBinFolder.exists() || !openOcdBinFolder.isDirectory()) {
            openOcdNotFound();
        }
        File openOcdExe = null;
        List<String> extensions = OS.isWindows() ? PathEnvironmentVariableUtil.getWindowsExecutableFileExtensions() : Collections.singletonList("");
        for (String ext : extensions) {
            File exePretender = new File(openOcdBinFolder, "openocd" + ext);
            if (exePretender.canExecute()) {
                openOcdExe = exePretender;
                break;
            }
        }
        if (openOcdExe == null) {
            return openOcdNotFound();
        }
        GeneralCommandLine commandLine = new PtyCommandLine()
                .withWorkDirectory(openOcdBinFolder)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withParameters("-c", "tcl_port disabled")
                .withExePath(openOcdExe.getAbsolutePath());

        commandLine.addParameters("-s", ocdSettings.openOcdHome +
                File.separator + "share" + File.separator + "openocd" + File.separator + "scripts");
        if (ocdSettings.gdbPort != OpenOcdSettingsState.DEF_GDB_PORT) {
            commandLine.addParameters("-c", "gdb_port " + ocdSettings.gdbPort);
        }
        if (ocdSettings.telnetPort != OpenOcdSettingsState.DEF_TELNET_PORT) {
            commandLine.addParameters("-c", "telnet_port " + ocdSettings.telnetPort);
        }
        commandLine.addParameters("-f", ocdSettings.boardConfigFile);
        String command = "";
        if (fileToLoad != null) {
            command += "program \"" + fileToLoad.getAbsolutePath().replace(File.separatorChar, '/') + "\";";
        }
        if (additionalCommand != null) {
            command += additionalCommand + ";";
        }
        if (shutdown) {
            command += "shutdown";
        }
        if (!command.isEmpty()) {
            commandLine.addParameters("-c", command);
        }
        return commandLine;
    }

    private static GeneralCommandLine openOcdNotFound() throws ConfigurationException {
        throw new ConfigurationException("Please open settings dialog and fix OpenOCD home", "OpenOCD config error");
    }

    @SuppressWarnings("WeakerAccess")
    public void stopOpenOcd() {
        if (process == null || process.isProcessTerminated() || process.isProcessTerminating()) return;
        ProgressManager.getInstance().executeNonCancelableSection(() -> {
            process.destroyProcess();
            process.waitFor(1000);
        });
    }

    @SuppressWarnings("WeakerAccess")
    public Future<STATUS> startOpenOcd(Project project, @Nullable File fileToLoad, @Nullable String additionalCommand) throws ConfigurationException {
        if (project == null) return new FutureResult<>(STATUS.FLASH_ERROR);
        GeneralCommandLine commandLine = createOcdCommandLine(project, fileToLoad, additionalCommand, false);
        if (process != null && !process.isProcessTerminated()) {
            LOG.info("openOcd is already run");
            return new FutureResult<>(STATUS.FLASH_ERROR);
        }

        try {
            process = new OSProcessHandler(commandLine) {
                @Override
                public boolean isSilentlyDestroyOnClose() {
                    return true;
                }
            };
            DownloadFollower downloadFollower = new DownloadFollower();
            process.addProcessListener(downloadFollower);
            RunContentExecutor openOCDConsole = new RunContentExecutor(project, process)
                    .withTitle("OpenOCD console")
                    .withActivateToolWindow(true)
                    .withFilter(new ErrorFilter(project))
                    .withStop(process::destroyProcess,
                            () -> !process.isProcessTerminated() && !process.isProcessTerminating());

            openOCDConsole.run();
            return downloadFollower;
        } catch (ExecutionException e) {
            ExecutionErrorDialog.show(e, "OpenOCD start failed", project);
            return new FutureResult<>(STATUS.FLASH_ERROR);
        }
    }

    public boolean isRun() {
        return process != null && !process.isProcessTerminated();
    }

    public enum STATUS {
        FLASH_SUCCESS,
        FLASH_ERROR,
    }

    private class ErrorFilter implements Filter {
        private final Project project;

        ErrorFilter(Project project) {
            this.project = project;
        }

        /**
         * Filters line by creating an instance of {@link Result}.
         *
         * @param line         The line to be filtered. Note that the line must contain a line
         *                     separator at the end.
         * @param entireLength The length of the entire text including the line passed for filtration.
         * @return <tt>null</tt>, if there was no match, otherwise, an instance of {@link Result}
         */
        @Nullable
        @Override
        public Result applyFilter(String line, int entireLength) {
            if (line.startsWith(ERROR_PREFIX) || line.contains(FLASH_FAIL_TEXT)) {
                Informational.showFailedDownloadNotification(project);
                return new Result(0, line.length(), null,
                        myColorsScheme.getAttributes(ConsoleViewContentType.ERROR_OUTPUT_KEY)) {
                    @Override
                    public int getHighlighterLayer() {
                        return HighlighterLayer.ERROR;
                    }
                };
            } else if (line.contains(FLASH_SUCCESS_TEXT)) {
                Informational.showSuccessfulDownloadNotification(project);
            }
            return null;
        }
    }

    private class DownloadFollower extends FutureResult<STATUS> implements ProcessListener {
        @Override
        public void startNotified(@NotNull ProcessEvent event) {
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
            try {
                if (!isDone()) {
                    set(STATUS.FLASH_ERROR);
                }
            } catch (Exception e) {
                set(STATUS.FLASH_ERROR);
            }
        }

        @Override
        public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
            //nothing to do
        }

        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            String text = event.getText().trim();
            if (text.startsWith(ERROR_PREFIX) || text.equals(FLASH_FAIL_TEXT)) {
                reset();
                set(STATUS.FLASH_ERROR);
            } else if (text.equals(FLASH_SUCCESS_TEXT)) {
                reset();
                set(STATUS.FLASH_SUCCESS);
            }
        }
    }
}
