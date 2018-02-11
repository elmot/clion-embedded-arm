package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.backend.GDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess;
import com.jetbrains.cidr.execution.testing.CidrLauncher;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.openocd.OpenOcdComponent.STATUS;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * (c) elmot on 19.10.2017.
 */
class OpenOcdLauncher extends CidrLauncher {

    private static final Key<AnAction> RESTART_KEY = Key.create(OpenOcdLauncher.class.getName() + "#restartAction");
    private final OpenOcdConfiguration openOcdConfiguration;

    OpenOcdLauncher(OpenOcdConfiguration openOcdConfiguration) {
        this.openOcdConfiguration = openOcdConfiguration;
    }

    @Override
    protected ProcessHandler createProcess(@NotNull CommandLineState commandLineState) throws ExecutionException {
        File runFile = findRunFile(commandLineState);
        findOpenOcdAction(commandLineState.getEnvironment().getProject()).stopOpenOcd();
        try {
            GeneralCommandLine commandLine = OpenOcdComponent.createOcdCommandLine(commandLineState.getEnvironment().getProject(),
                    openOcdConfiguration, runFile, "reset", true);
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
            Messages.showErrorDialog(getProject(), e.getLocalizedMessage(), e.getTitle());
            throw new ExecutionException(e);
        }
    }

    @NotNull
    @Override
    protected CidrDebugProcess createDebugProcess(@NotNull CommandLineState commandLineState, @NotNull XDebugSession xDebugSession) throws ExecutionException {
        Project project = commandLineState.getEnvironment().getProject();
        OpenOcdSettingsState ocdSettings = project.getComponent(OpenOcdSettingsState.class);
        CidrRemoteDebugParameters remoteDebugParameters = new CidrRemoteDebugParameters();

        remoteDebugParameters.setSymbolFile(findRunFile(commandLineState).getAbsolutePath());

        int gdbPort = OpenOcdConfiguration.actualGdbPort(openOcdConfiguration, ocdSettings);
        remoteDebugParameters.setRemoteCommand("tcp:localhost:" + gdbPort);

        CPPToolchains.Toolchain toolchain = CPPToolchains.getInstance().getDefaultToolchain();
        if (toolchain != null && !ocdSettings.shippedGdb) {
            toolchain = toolchain.copy();
            toolchain.setDebuggerKind(CPPToolchains.DebuggerKind.CUSTOM_GDB);
            toolchain.setCustomGDBExecutablePath(ocdSettings.gdbLocation);
        }
        GDBDriverConfiguration gdbDriverConfiguration = new GDBDriverConfiguration(getProject(), toolchain);
        xDebugSession.stop();
        CidrRemoteGDBDebugProcess debugProcess = new CidrRemoteGDBDebugProcess(gdbDriverConfiguration, remoteDebugParameters, xDebugSession, commandLineState.getConsoleBuilder());
        debugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                super.processWillTerminate(event, willBeDestroyed);
                findOpenOcdAction(project).stopOpenOcd();
            }
        });
        debugProcess.getProcessHandler().putUserData(RESTART_KEY,
                new AnAction("Reset", "MCU Reset", IconLoader.findIcon("reset.png", OpenOcdLauncher.class)) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        XDebugSession session = debugProcess.getSession();
                        session.pause();
                        debugProcess.postCommand(drv -> {
                            try {
                                ProgressManager.getInstance().runProcess(() -> {
                                    while (drv.getState() != DebuggerDriver.TargetState.SUSPENDED) {
                                        Thread.yield();
                                    }
                                }, null);
                                drv.executeConsoleCommand("monitor reset init");
                                session.resume();
                            } catch (DebuggerCommandException exception) {
                                Informational.showFailedDownloadNotification(e.getProject());
                            }
                        });
                    }
                }
        );

        return debugProcess;
    }

    @NotNull
    private File findRunFile(CommandLineState commandLineState) throws ExecutionException {
        String targetProfileName = commandLineState.getExecutionTarget().getDisplayName();
        CMakeAppRunConfiguration.BuildAndRunConfigurations runConfigurations = openOcdConfiguration.getBuildAndRunConfigurations(targetProfileName);
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


    @NotNull
    @Override
    public CidrDebugProcess startDebugProcess(@NotNull CommandLineState commandLineState, @NotNull XDebugSession xDebugSession) throws ExecutionException {
        Project project = commandLineState.getEnvironment().getProject();

        File runFile = findRunFile(commandLineState);

        try {
            xDebugSession.stop();
            OpenOcdComponent openOcdComponent = findOpenOcdAction(commandLineState.getEnvironment().getProject());
            openOcdComponent.stopOpenOcd();
            Future<STATUS> downloadResult = openOcdComponent.startOpenOcd(project, openOcdConfiguration, runFile, "reset init");

            ThrowableComputable<STATUS, ExecutionException> process = () -> {
                try {
                    ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                    return downloadResult.get(10, TimeUnit.MINUTES);
                } catch (InterruptedException | TimeoutException | java.util.concurrent.ExecutionException e) {
                    throw new ExecutionException(e);
                }
            };
            STATUS downloadStatus = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    process, "Firmware Download", true, getProject());
            if (downloadStatus == STATUS.FLASH_ERROR) {
                downloadResult.cancel(true);
                throw new ExecutionException("OpenOCD cancelled");
            }
            return super.startDebugProcess(commandLineState, xDebugSession);
        } catch (ConfigurationException e) {
            Messages.showErrorDialog(getProject(), e.getLocalizedMessage(), e.getTitle());
            throw new ExecutionException(e);
        }
    }

    @Override
    protected void collectAdditionalActions(@NotNull CommandLineState commandLineState, @NotNull ProcessHandler processHandler, @NotNull ExecutionConsole executionConsole, @NotNull List<AnAction> list) throws ExecutionException {
        super.collectAdditionalActions(commandLineState, processHandler, executionConsole, list);
        AnAction restart = processHandler.getUserData(RESTART_KEY);
        if (restart != null) {
            list.add(restart);
        }
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
