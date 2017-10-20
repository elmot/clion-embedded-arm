package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.ExecutionErrorDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * (c) elmot on 19.10.2017.
 */
public class OpenOcdRun extends AnAction {
    private final EditorColorsScheme myColorsScheme;
    private OSProcessHandler process;
    private static final Logger LOG = Logger.getInstance(OpenOcdRun.class);

    /**
     * Creates a new action with its text, description and icon set to {@code null}.
     */
    public OpenOcdRun() {
        myColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
    }

    /**
     * Implement this method to provide your action handler.
     *
     * @param event Carries information on the invocation place
     */

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        startOpenOcd(project, null, null);

    }

    @SuppressWarnings("WeakerAccess")
    public void stopOpenOcd() {
        if (process == null || process.isProcessTerminated() || process.isProcessTerminating()) return;
        process.destroyProcess();
        process.waitFor(1000);
    }

    @SuppressWarnings("WeakerAccess")
    public void startOpenOcd(Project project, @Nullable File fileToLoad, @Nullable String additionalCommand) {
        if (project == null) return;
        GeneralCommandLine commandLine = createOcdCommandLine(project, fileToLoad, additionalCommand, false);
        if (process != null && !process.isProcessTerminated()) {
            LOG.info("openOcd is already run");
            return;
        }


        try {
            process = new OSProcessHandler(commandLine);
            RunContentExecutor openOCDConsole = new RunContentExecutor(project, process)
                    .withTitle("OpenOCD console")
                    .withActivateToolWindow(true)
                    .withFilter(new ErrorFilter(project))
                    .withStop(process::destroyProcess,
                            () -> !process.isProcessTerminated() && !process.isProcessTerminating());

            openOCDConsole.run();
            //todo color console
            //todo process exit report
            //todo restart

        } catch (ExecutionException e) {
            ExecutionErrorDialog.show(e, "OpenOCD start failed", project);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public static GeneralCommandLine createOcdCommandLine(@NotNull Project project, @Nullable File fileToLoad, @Nullable String additionalCommand, boolean shutdown) {
        OpenOcdSettingsState ocdSettings = project.getComponent(OpenOcdSettingsState.class);
        GeneralCommandLine commandLine = new GeneralCommandLine()
                .withRedirectErrorStream(true)
                .withWorkDirectory(project.getBasePath())
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withExePath(ocdSettings.openOcdLocation)
                .withParameters("-f", ocdSettings.boardConfigFile);
        String command = "";
        if (fileToLoad != null) {
            command += "program \"" + fileToLoad.getAbsolutePath() + "\";";
        }
        if (additionalCommand != null) {
            command += additionalCommand + ";";
        }
        if (shutdown) {
            command += "shutdown";
        }
        if (!command.isEmpty()) {
            commandLine.withParameters("-c", command);
        }
        return commandLine;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(process == null || process.isProcessTerminated());
        e.getPresentation().setVisible(true);
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
            if (line.startsWith("Error: ") || line.contains("** Programming Failed **")) {
                Informational.showFailedDownloadNotification(project);
                return new Result(0, line.length(), null,
                        myColorsScheme.getAttributes(ConsoleViewContentType.ERROR_OUTPUT_KEY)) {
                    @Override
                    public int getHighlighterLayer() {
                        return HighlighterLayer.ERROR;
                    }
                };
            } else if(line.contains("** Programming Finished **")){
                Informational.showSuccessfulDownloadNotification(project);
            }
            return null;
        }
    }
}
