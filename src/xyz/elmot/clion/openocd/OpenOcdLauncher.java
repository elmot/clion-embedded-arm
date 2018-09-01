package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.Filter;
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
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.backend.GDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPDebugger;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.CidrDebuggerPathManager;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess;
import com.jetbrains.cidr.execution.testing.CidrLauncher;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.openocd.OpenOcdComponent.STATUS;
import xyz.elmot.clion.openocd.OpenOcdConfiguration.UploadType;

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
                        Informational.showSuccessfulUploadNotification(project);
                    } else {
                        Informational.showFailedUploadNotification(project);
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
            File gdbFile = CidrDebuggerPathManager.getBundledGDBBinary();
            gdbPath = gdbFile.getAbsolutePath();
            CPPDebugger cppDebugger = CPPDebugger.create(CPPDebugger.Kind.CUSTOM_GDB, gdbPath);
            toolchain.setDebugger(cppDebugger);
        }
        GDBDriverConfiguration gdbDriverConfiguration = new GDBDriverConfiguration(getProject(), toolchain);
        xDebugSession.stop();
        CidrRemoteGDBDebugProcess debugProcess =
                new CidrRemoteGDBDebugProcess(gdbDriverConfiguration,
                        remoteDebugParameters,
                        xDebugSession,
                        commandLineState.getConsoleBuilder(),
                        project1 -> new Filter[0]);
        debugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                super.processWillTerminate(event, willBeDestroyed);
                findOpenOcdAction(project).stopOpenOcd();
            }
        });
        debugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                super.startNotified(event);
                onStartDebugProcess(debugProcess);
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
                                Informational.showFailedUploadNotification(e.getProject());
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


    @NotNull
    @Override
    public CidrDebugProcess startDebugProcess(@NotNull CommandLineState commandLineState,
                                              @NotNull XDebugSession xDebugSession) throws ExecutionException {

        File runFile = null;
        if (openOcdConfiguration.getUploadType() != UploadType.NONE) {
            runFile = findRunFile(commandLineState);
            if (openOcdConfiguration.getUploadType() == UploadType.UPDATED_ONLY &&
                    OpenOcdComponent.isLatestUploaded(runFile)) {
                runFile = null;
            }
        }

        try {
            xDebugSession.stop();
            Project project = commandLineState.getEnvironment().getProject();
            OpenOcdComponent openOcdComponent = findOpenOcdAction(project);
            openOcdComponent.stopOpenOcd();
            Future<STATUS> uploadResult = openOcdComponent.startOpenOcd(openOcdConfiguration, runFile, openOcdConfiguration.getResetCommand());

            ProgressManager progressManager = ProgressManager.getInstance();
            ThrowableComputable<STATUS, ExecutionException> process = () -> {
                try {
                    progressManager.getProgressIndicator().setIndeterminate(true);
                    while (true) {
                        try {
                            return uploadResult.get(500, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException ignored) {
                            ProgressManager.checkCanceled();
                        }
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    throw new ExecutionException(e);
                }
            };
            String progressTitle = runFile == null ? "Start OpenOCD" : "Firmware Upload";
            STATUS uploadStatus = progressManager.runProcessWithProgressSynchronously(
                    process, progressTitle, true, getProject());
            if (uploadStatus == STATUS.FLASH_ERROR) {
                uploadResult.cancel(true);
                throw new ExecutionException("OpenOCD cancelled");
            }
            return super.startDebugProcess(commandLineState, xDebugSession);
        } catch (ConfigurationException e) {
            Informational.showPluginError(getProject(), e);
            throw new ExecutionException(e);
        }
    }

    private void onStartDebugProcess(@NotNull CidrDebugProcess debugProcess) {
        final XDebugSession session = debugProcess.getSession();
        if (session == null) return;
        session.pause(); // gdb ^C
        debugProcess.postCommand(drv -> {
            try {
                ProgressManager.getInstance().runProcess(() -> {
                    while (drv.getState() != DebuggerDriver.TargetState.SUSPENDED) {
                        Thread.yield();
                    }
                }, null);
                // this script ensures we execute the C/C++ runtime init code before we start debugging
                drv.executeConsoleCommand("b main");
                session.resume(); // gdb "cont"
            } catch (DebuggerCommandException e) {
                Informational.showMessage(getProject(), MessageType.ERROR,
                                "Failed to execute startup GDB script: " + e);
            }
        });
    }

    @Override
    protected void collectAdditionalActions(@NotNull CommandLineState commandLineState,
                                            @NotNull ProcessHandler processHandler,
                                            @NotNull ExecutionConsole executionConsole, @NotNull List<AnAction> list)
            throws ExecutionException {
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
