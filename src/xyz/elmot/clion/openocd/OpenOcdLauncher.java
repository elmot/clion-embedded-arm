package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.backend.GDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPDebugger;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters;
import com.jetbrains.cidr.execution.testing.CidrLauncher;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.openocd.OpenOcdComponent.STATUS;

import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * (c) elmot on 19.10.2017.
 */
class OpenOcdLauncher extends CidrLauncher {

    private final OpenOcdConfiguration openOcdConfiguration;

    public OpenOcdLauncher(OpenOcdConfiguration openOcdConfiguration) {
        this.openOcdConfiguration = openOcdConfiguration;
    }

    @Override
    protected ProcessHandler createProcess(@NotNull CommandLineState commandLineState) throws ExecutionException {
        File runFile = findRunFile(commandLineState);
        findOpenOcdAction(commandLineState.getEnvironment().getProject()).stopOpenOcd();
        try {
            GeneralCommandLine commandLine = OpenOcdComponent
                    .createOcdCommandLine(openOcdConfiguration,
                            runFile, "reset", true);
            OSProcessHandler osProcessHandler = new OSProcessHandler(commandLine);
            osProcessHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    super.processTerminated(event);
                    Project project = commandLineState.getEnvironment().getProject();
                    if (event.getExitCode() == 0) {
                        Informational.showSuccessfulDownloadNotification(project);
                    } else {
                        Informational.showFailedDownloadNotification(project);
                    }
                }
            });
            return osProcessHandler;
        } catch (ConfigurationException e) {
            Informational.showPluginError(getProject(), e);
            throw new ExecutionException(e);
        }
    }

    @NotNull
    @Override
    protected CidrDebugProcess createDebugProcess(@NotNull CommandLineState commandLineState,
                                                  @NotNull XDebugSession xDebugSession) throws ExecutionException {
        Project project = commandLineState.getEnvironment().getProject();
        OpenOcdSettingsState ocdSettings = project.getComponent(OpenOcdSettingsState.class);
        CidrRemoteDebugParameters remoteDebugParameters = new CidrRemoteDebugParameters();

        remoteDebugParameters.setSymbolFile(findRunFile(commandLineState).getAbsolutePath());
        remoteDebugParameters.setRemoteCommand("tcp:localhost:" + openOcdConfiguration.getGdbPort());

        CPPToolchains.Toolchain toolchain = CPPToolchains.getInstance().getDefaultToolchain();
        if (toolchain == null) {
            throw new ExecutionException("Project toolchain is not defined. Please define it in the project settings.");
        }
        String gdbPath;
        if (ocdSettings.shippedGdb) {
            toolchain = toolchain.copy();
            File gdbFile = PathManager.findBinFile("gdb/bin/gdb" + (OS.isWindows() ? ".exe" : ""));
            if (gdbFile == null) {
                throw new ExecutionException("Shipped gdb is not found. Please check your CLion install");
            }
            gdbPath = gdbFile.getAbsolutePath();
            CPPDebugger cppDebugger = CPPDebugger.create(CPPDebugger.Kind.CUSTOM_GDB, gdbPath);
            toolchain.setDebugger(cppDebugger);
        }
        GDBDriverConfiguration gdbDriverConfiguration = new GDBDriverConfiguration(project, toolchain);
        xDebugSession.stop();
        File runFile = null;
        if (openOcdConfiguration.getDownloadType() != OpenOcdConfiguration.DownloadType.NONE) {
            runFile = findRunFile(commandLineState);
            if (openOcdConfiguration.getDownloadType() == OpenOcdConfiguration.DownloadType.UPDATED_ONLY &&
                    OpenOcdComponent.isLatestUploaded(runFile)) {
                runFile = null;
            }
        }

        ConsoleView openOcdConsole = commandLineState.getConsoleBuilder().getConsole();
        try {
            xDebugSession.stop();
            OpenOcdComponent openOcdComponent = findOpenOcdAction(commandLineState.getEnvironment().getProject());
            openOcdComponent.stopOpenOcd();
            Future<STATUS> downloadResult = openOcdComponent.startOpenOcd(openOcdConfiguration, runFile, openOcdConfiguration.getResetType().getCommand(), openOcdConsole);

            ProgressManager progressManager = ProgressManager.getInstance();
            ThrowableComputable<STATUS, ExecutionException> process = () -> {
                try {
                    progressManager.getProgressIndicator().setIndeterminate(true);
                    while (true) {
                        try {
                            return downloadResult.get(500, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException ignored) {
                            ProgressManager.checkCanceled();
                        }
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    throw new ExecutionException(e);
                }
            };
            String progressTitle = runFile == null ? "Start OpenOCD" : "Firmware Download";
            STATUS downloadStatus = progressManager.runProcessWithProgressSynchronously(
                    process, progressTitle, true, getProject());
            if (downloadStatus == STATUS.FLASH_ERROR) {
                downloadResult.cancel(true);
                throw new ExecutionException("OpenOCD cancelled");
            }
        } catch (ConfigurationException e) {
            Informational.showPluginError(getProject(), e);
            throw new ExecutionException(e);
        }

        return new OpenOcdGDBDebugProcess(toolchain,
                remoteDebugParameters,
                xDebugSession,
                commandLineState.getConsoleBuilder(), findOpenOcdAction(project)
        ,openOcdConsole);
    }

    @NotNull
    private File findRunFile(CommandLineState commandLineState) throws ExecutionException {
        String targetProfileName = commandLineState.getExecutionTarget().getDisplayName();
        CMakeAppRunConfiguration.BuildAndRunConfigurations runConfigurations = openOcdConfiguration
                .getBuildAndRunConfigurations(targetProfileName);
        if (runConfigurations == null) {
            throw new ExecutionException("Target is not defined");
        }
        File runFile = runConfigurations.getRunFile();
        if (runFile == null) {
            throw new ExecutionException("Run file is not defined for " + runConfigurations);
        }
        if (!runFile.exists() || !runFile.isFile()) {
            throw new ExecutionException("Invalid run file " + runFile.getAbsolutePath());
        }
        return runFile;
    }


    private OpenOcdComponent findOpenOcdAction(Project project) {
        return project.getComponent(OpenOcdComponent.class);
    }


    @NotNull
    @Override
    protected Project getProject() {
        return openOcdConfiguration.getProject();
    }

}
