package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import com.jetbrains.cidr.cpp.execution.debugger.backend.GDBDriverConfiguration;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess;
import org.jetbrains.annotations.NotNull;

public class OpenOcdGDBDebugProcess extends CidrRemoteGDBDebugProcess {

    private final ConsoleView openOcdConsole;

    public OpenOcdGDBDebugProcess(GDBDriverConfiguration gdbDriverConfiguration,
                                  CidrRemoteDebugParameters remoteDebugParameters,
                                  XDebugSession xDebugSession,
                                  TextConsoleBuilder consoleBuilder, final OpenOcdComponent openOcdAction, ConsoleView openOcdConsole) throws ExecutionException {
        super(gdbDriverConfiguration, remoteDebugParameters, xDebugSession, consoleBuilder,
                project1 -> new Filter[0]);
        getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                super.processWillTerminate(event, willBeDestroyed);
                openOcdAction.stopOpenOcd();
            }
        });
        this.openOcdConsole = openOcdConsole;
    }

    @NotNull
    @Override
    public XDebugTabLayouter createTabLayouter() {
        XDebugTabLayouter tabLayouter = super.createTabLayouter();
        return new XDebugTabLayouter() {
            @Override
            public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
                tabLayouter.registerAdditionalContent(ui);
                Content content = ui.createContent("OPENOCD_CONSOLE", openOcdConsole.getComponent(), "OpenOCD Console",
                        IconLoader.getIcon("ocd.png",OpenOcdGDBDebugProcess.class),
                        openOcdConsole.getPreferredFocusableComponent());
//                Content content = ui.createContent("TestContent", new JPanel(), "TestContent", AllIcons.Toolwindows.ToolWindowAnt, null);
                ui.addContent(content);
            }
        };
    }

    @Override
    public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar, @NotNull DefaultActionGroup topToolbar, @NotNull DefaultActionGroup settings) {
        super.registerAdditionalActions(leftToolbar, topToolbar, settings);
        XDebugProcess debugProcess = getSession().getDebugProcess();
        AnAction resetAction = new AnAction("Reset", "MCU Reset", IconLoader.findIcon("reset.png", OpenOcdLauncher.class)) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                XDebugSession session = debugProcess.getSession();
                session.pause();
                postCommand(drv -> {
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
        };
        leftToolbar.addAction(resetAction, new Constraints(Anchor.FIRST, null));
    }

}
