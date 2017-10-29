package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CLionRunParameters;
import com.jetbrains.cidr.cpp.execution.debugger.backend.GDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.TrivialInstaller;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
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
        findOpenOcdAction(commandLineState.getEnvironment().getProject()).stopOpenOcd();
        GeneralCommandLine commandLine = OpenOcdComponent.createOcdCommandLine(commandLineState.getEnvironment().getProject(),
                runFile, "reset init", false, true);
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

        CPPToolchains.Toolchain defaultToolchain = CPPToolchains.getInstance().getDefaultToolchain();//todo project specific toolchain
        GDBDriverConfiguration gdbDriverConfiguration = new GDBDriverConfiguration(getProject(), defaultToolchain, new File(ocdSettings.gdbLocation));
        CLionRunParameters cLionRunParameters = new CLionRunParameters(gdbDriverConfiguration,
                new TrivialInstaller(new GeneralCommandLine(openOcdConfiguration.findRunFile().getAbsolutePath())));
        return new OpenOcdDebugProcess(cLionRunParameters,xDebugSession,commandLineState.getConsoleBuilder());
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
