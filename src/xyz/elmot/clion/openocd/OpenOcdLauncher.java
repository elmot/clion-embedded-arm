package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
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
        remoteDebugParameters.setRemoteCommand("tcp:localhost:" + ocdSettings.gdbPort);

        CPPToolchains.Toolchain defaultToolchain = CPPToolchains.getInstance().getDefaultToolchain();
        GDBDriverConfiguration gdbDriverConfiguration = new GDBDriverConfiguration(getProject(), defaultToolchain, new File(ocdSettings.gdbLocation));
        CidrRemoteGDBDebugProcess debugProcess = new CidrRemoteGDBDebugProcess(gdbDriverConfiguration, remoteDebugParameters, xDebugSession, commandLineState.getConsoleBuilder());
        debugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                super.processWillTerminate(event, willBeDestroyed);
                findOpenOcdAction(project).stopOpenOcd();
            }
        });
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
            findOpenOcdAction(commandLineState.getEnvironment().getProject()).startOpenOcd(project, runFile, "reset halt");
            return super.startDebugProcess(commandLineState, xDebugSession);
        } catch (ConfigurationException e) {
            Messages.showErrorDialog(getProject(), e.getLocalizedMessage(), e.getTitle());
            throw new ExecutionException(e);
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
