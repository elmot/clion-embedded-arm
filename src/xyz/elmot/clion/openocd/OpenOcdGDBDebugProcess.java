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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import com.jetbrains.cidr.cpp.execution.debugger.backend.GDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.backend.LLValue;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class OpenOcdGDBDebugProcess extends CidrRemoteGDBDebugProcess {

    private final ConsoleView openOcdConsole;
    private LineChart<Number, Number> lineChart;

    public OpenOcdGDBDebugProcess(CPPToolchains.Toolchain toolchain,
                                  CidrRemoteDebugParameters remoteDebugParameters,
                                  XDebugSession xDebugSession,
                                  TextConsoleBuilder consoleBuilder, final OpenOcdComponent openOcdAction, ConsoleView openOcdConsole) throws ExecutionException {
        super(new MyGdbDriverConfiguration(xDebugSession.getProject(),toolchain), remoteDebugParameters, xDebugSession, consoleBuilder,
                project1 -> new Filter[0]);
        this.openOcdConsole = openOcdConsole;
        getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                super.processWillTerminate(event, willBeDestroyed);
                openOcdAction.stopOpenOcd();
            }
        });
        xDebugSession.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
/*
                ((CidrDebugProcess) xDebugSession.getDebugProcess()).postCommand(
                        debuggerDriver -> {
                            try {
                                debuggerDriver.evaluate(100,0,"buffer[0]");
                            } catch (DebuggerCommandException e) {
                                e.printStackTrace();
                            }
                        }
                );
*/
            }
        });
    }

    @NotNull
    @Override
    public XDebugTabLayouter createTabLayouter() {
        XDebugTabLayouter tabLayouter = super.createTabLayouter();
        return new XDebugTabLayouter() {
            @Override
            public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
                tabLayouter.registerAdditionalContent(ui);
                ui.addContent(createOOCDConsoleContent(ui));
//                ui.addContent(createChartContent(ui));
            }
        };
    }

    private Content createChartContent(RunnerLayoutUi ui) {
        JFXPanel fxPanel = new JFXPanel();
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {

            //defining the axes
            final NumberAxis xAxis = new NumberAxis();
            final NumberAxis yAxis = new NumberAxis();
            //creating the chart
            lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setAnimated(false);

            Scene scene = new Scene(lineChart);

            fxPanel.setScene(scene);
            fxPanel.invalidate();
        });
        fxPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Platform.runLater(fxPanel::invalidate);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                fxPanel.invalidate();
            }
        });
        return ui.createContent("OPENOCD_CHART", fxPanel, "Signal Chart", null, null);

    }

    @NotNull
    //todo console actions?
    //todo unify console??
    //todo follow openocd failures/terminates
    //todo keep singleещт on all the time
    private Content createOOCDConsoleContent(@NotNull RunnerLayoutUi ui) {
        Content content = ui.createContent("OPENOCD_CONSOLE", openOcdConsole.getComponent(), "OpenOCD Console",
                IconLoader.getIcon("ocd.png", OpenOcdGDBDebugProcess.class),
                openOcdConsole.getPreferredFocusableComponent());
        content.setCloseable(false);
        return content;
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
