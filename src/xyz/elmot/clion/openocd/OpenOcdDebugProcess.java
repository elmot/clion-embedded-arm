package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Disposer;
import com.intellij.terminal.TerminalExecutionConsole;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import com.jetbrains.cidr.execution.RunParameters;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * (c) elmot on 28.10.2017.
 */
public class OpenOcdDebugProcess extends CidrDebugProcess {
    private static final String OPEN_OCD_ID = "elmot.embedded.openocd.runner";
    private OSProcessHandler ocdHandler;
    private DebuggerDriver debuggerDriver;
    private TerminalExecutionConsole ocdConsole;

    @SuppressWarnings("WeakerAccess")
    public OpenOcdDebugProcess(@NotNull RunParameters runParameters, @NotNull XDebugSession xDebugSession, @NotNull TextConsoleBuilder textConsoleBuilder) throws ExecutionException {
        super(runParameters, xDebugSession, textConsoleBuilder);
        File executableFile = getRunParameters().getInstaller().getExecutableFile();
        ocdHandler = new OSProcessHandler(OpenOcdComponent.createOcdCommandLine(getProject(),
                executableFile, null, true, false));
        getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                try {
                    debuggerDriver.executeConsoleCommand("monitor shutdown");
                } catch (ExecutionException | DebuggerCommandException e) {
                    throw new RuntimeException(e);
                }
                super.processWillTerminate(event, willBeDestroyed);
            }
        });
        ocdConsole = new TerminalExecutionConsole(getProject(), ocdHandler);
    }

    @NotNull
    @Override
    protected DebuggerDriver.Inferior doLoadTarget(@NotNull DebuggerDriver driver) throws ExecutionException {
/*
        postCommand((drv) -> {
        });
*/
        return driver.loadForLaunch(getRunParameters().getInstaller(), null);
    }

    @Override
    protected long doStartTarget(@NotNull DebuggerDriver.Inferior inferior) throws ExecutionException {

        debuggerDriver = inferior.getDriver();
        DebuggerDriver drv = debuggerDriver;
        try {
            drv.executeConsoleCommand("target remote tcp:localhost:3333");//todo parameter
            drv.executeConsoleCommand("monitor program \"" + getRunParameters().getInstaller().getExecutableFile().getAbsolutePath() + "\"");
            drv.executeConsoleCommand("monitor reset init");
            drv.executeConsoleCommand("continue");
        } catch (DebuggerCommandException e) {
            throw new ExecutionException(e);
        }
/*
        try {
            debuggerDriver = inferior.getDriver();
            debuggerDriver.executeConsoleCommand("monitor reset run");
        } catch (DebuggerCommandException e) {
            throw new ExecutionException(e);
        }

*/
        return 0;
    }

    @NotNull
    @Override
    public XDebugTabLayouter createTabLayouter() {
        XDebugTabLayouter tabLayouter = super.createTabLayouter();
        RunnerLayoutUi ui = RunnerLayoutUi.Factory.getInstance(getProject())
                .create(OPEN_OCD_ID, "OpenOCD", getSession().getSessionName(),
                        getProject());//todo what to put here???
        Content content = ui.createContent(OPEN_OCD_ID, ocdConsole.getComponent(),
                "xyz.elmot.embedded", AllIcons.Debugger.ToolConsole, null);/**/
        Disposer.register(ui.getContentManager(), ocdConsole);

        content.setCloseable(false);

        ui.addContent(content, 0, PlaceInGrid.center, false);
        tabLayouter.registerConsoleContent(ui,ocdConsole);
        return tabLayouter;
    }

    @Override
    public void stop() {
        if (ocdHandler != null && !ocdHandler.isProcessTerminated()) {
            try {
                ocdHandler.getProcess().destroy();
                ocdHandler.getProcess().waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                ocdHandler.getProcess().destroyForcibly();
            }
        }
        super.stop();
    }
}
