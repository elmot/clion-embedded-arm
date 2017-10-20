package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.debugger.backend.GDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess;
import com.jetbrains.cidr.execution.testing.CidrLauncher;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * (c) elmot on 19.10.2017.
 */
class OpenOcdLauncher extends CidrLauncher {

    private OpenOcdConfiguration openOcdConfiguration;

    OpenOcdLauncher(OpenOcdConfiguration openOcdConfiguration) {
        this.openOcdConfiguration = openOcdConfiguration;
    }

    @Override
    protected ProcessHandler createProcess(@NotNull CommandLineState commandLineState) throws ExecutionException {
        File runFile = openOcdConfiguration.findRunFile();
        findOpenOcdAction().stopOpenOcd();
        GeneralCommandLine commandLine = OpenOcdRun.createOcdCommandLine(commandLineState.getEnvironment().getProject(),
                runFile, "reset init", true);
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
    }

    @NotNull
    @Override
    protected CidrDebugProcess createDebugProcess(@NotNull CommandLineState commandLineState, @NotNull XDebugSession xDebugSession) throws ExecutionException {
        Project project = commandLineState.getEnvironment().getProject();
        OpenOcdSettingsState ocdSettings = project.getComponent(OpenOcdSettingsState.class);
        CidrRemoteDebugParameters remoteDebugParameters = new CidrRemoteDebugParameters();
        remoteDebugParameters.setSymbolFile(openOcdConfiguration.findRunFile().getAbsolutePath());
        remoteDebugParameters.setRemoteCommand("tcp:localhost:" + ocdSettings.gdbPort);

        CPPToolchains.Toolchain defaultToolchain = CPPToolchains.getInstance().getDefaultToolchain();
        GDBDriverConfiguration gdbDriverConfiguration = new GDBDriverConfiguration(getProject(), defaultToolchain, new File(ocdSettings.gdbLocation));
        CidrRemoteGDBDebugProcess debugProcess = new CidrRemoteGDBDebugProcess(gdbDriverConfiguration, remoteDebugParameters, xDebugSession, commandLineState.getConsoleBuilder());
        debugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                super.processWillTerminate(event, willBeDestroyed);
                findOpenOcdAction().stopOpenOcd();
            }
        });
        return debugProcess;
    }

    @NotNull
    @Override
    public CidrDebugProcess startDebugProcess(@NotNull CommandLineState commandLineState, @NotNull XDebugSession xDebugSession) throws ExecutionException {
        Project project = commandLineState.getEnvironment().getProject();
        File runFile = openOcdConfiguration.findRunFile();

        findOpenOcdAction().startOpenOcd(project, runFile, "reset halt");
        return super.startDebugProcess(commandLineState, xDebugSession);
    }

    private OpenOcdRun findOpenOcdAction() {
        return (OpenOcdRun) ActionManager.getInstance().getAction("openocd.run");
    }


    @NotNull
    @Override
    protected Project getProject() {
        return openOcdConfiguration.getProject();
    }

}
