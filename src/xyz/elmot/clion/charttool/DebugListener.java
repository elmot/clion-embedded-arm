package xyz.elmot.clion.charttool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.frame.XStackFrame;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.CidrEvaluator;
import org.antlr.v4.runtime.misc.NotNull;
import org.jetbrains.concurrency.Promise;
import xyz.elmot.clion.charttool.state.ChartExpr;
import xyz.elmot.clion.charttool.state.ExpressionState;
import xyz.elmot.clion.charttool.state.LineState;
import xyz.elmot.clion.openocd.Informational;
import xyz.elmot.clion.openocd.OpenOcdConfigurationType;
import xyz.elmot.clion.openocd.OpenOcdLauncher;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static xyz.elmot.clion.charttool.ChartTool.CHART_EXPR_KEY;
import static xyz.elmot.clion.charttool.SignalSources.getAllXLineBreakpoints;

public class DebugListener implements XDebugSessionListener {
    private static final Pattern ARRAY_STRIPPER = Pattern.compile("^[^{]*\\{|\\s+|}$");
    private final Project project;
    private final ChartsPanel chartsPanel;
    private final XDebuggerManager debuggerManager;
    private ChartToolPersistence persistence;

    public DebugListener(Project project, ChartsPanel chartsPanel, ChartToolPersistence persistence) {
        this.project = project;
        this.chartsPanel = chartsPanel;
        debuggerManager = XDebuggerManager.getInstance(project);
        this.persistence = persistence;
    }

    protected void showError(Throwable rejected, String chartExpression) {
        String message = chartExpression + ": " + rejected.getLocalizedMessage();
        String title = rejected.getClass().getSimpleName();
        ApplicationManager.getApplication().invokeLater(() ->
                com.intellij.openapi.ui.Messages.showErrorDialog(
                        message, title));
    }

    @Override
    public void sessionPaused() {
        XDebugSession session = debuggerManager.getCurrentSession();
        if (session == null) {
            return;
        }
        XStackFrame currentStackFrame = session.getCurrentStackFrame();
        if (currentStackFrame == null) {
            return;
        }
        XSourcePosition currentPosition = session.getCurrentPosition();
        if (currentPosition == null) {
            return;
        }
        CidrEvaluator evaluator = (CidrEvaluator) currentStackFrame.getEvaluator();
        if (evaluator == null) {
            return;
        }
        List<XLineBreakpoint<?>> allXLineBreakpoints = getAllXLineBreakpoints(project);
        for (XLineBreakpoint<?> breakpoint : allXLineBreakpoints) {
            if (XSourcePosition.isOnTheSameLine(currentPosition, breakpoint.getSourcePosition())) {
                LineState lineState = breakpoint.getUserData(CHART_EXPR_KEY);
                if (lineState != null) {
                    if (lineState.clearChart) {
                        chartsPanel.clear();
                    }
                    if (lineState.sample) {
                        sampleChart((CidrDebugProcess) session.getDebugProcess());
                    }
                    if (lineState.autoResume) {
                        session.resume();
                    }
                }
            }
        }
    }

    private void sampleChart(@NotNull CidrDebugProcess debugProcess) {
        for (ChartExpr chartExpr : persistence.getExprs()) {
            String expressionTrim = chartExpr.getExpressionTrim();
            if (chartExpr.getState() == ExpressionState.DISABLED ||
                    expressionTrim.isEmpty() ||
                    (chartExpr.getState() == ExpressionState.SAMPLE_ONCE
                            && chartsPanel.isSampled(chartExpr.getName()))) {
                continue;
            }
            Promise<String> sinDataPromise = debugProcess.postCommand(debuggerDriver ->
                    (((OpenOcdLauncher.ExtendedGdbDriver) debuggerDriver)
                            .requestValue("p/r " + expressionTrim))
            );
            try {
                String evalResult = sinDataPromise
                        .blockingGet(20, TimeUnit.SECONDS);
                processGdbOutput(evalResult, chartExpr);
            } catch (ExecutionException e) {
                ApplicationManager.getApplication().invokeLater(() ->
                        {
                            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                            if (toolWindowManager.canShowNotification(ToolWindowId.RUN)) {
                                Throwable t = e;
                                while (t.getCause() != null) {
                                    t = t.getCause();
                                }
                                String localizedMessage = t.getLocalizedMessage();
                                toolWindowManager.notifyByBalloon(ToolWindowId.DEBUG, MessageType.WARNING,
                                        expressionTrim + ": " + localizedMessage,
                                        OpenOcdConfigurationType.getPluginIcon(), null
                                );
                            }
                        }
                );
            } catch (Throwable e) {
                showError(e, expressionTrim);
            }

        }
    }

    private void processGdbOutput(String v, ChartExpr chartExpr) {
        if (v != null) {
            try {
                String strippedV = ARRAY_STRIPPER.matcher(v).replaceAll("");
                List<Number> data = new ArrayList<>();
                int i = 1;
                for (StringTokenizer tokenizer = new StringTokenizer(strippedV, ",");
                     tokenizer.hasMoreTokens(); ) {
                    data.add(Double.parseDouble(tokenizer.nextToken()));
                }
                chartsPanel.series(chartExpr, data);
            } catch (Throwable e) {
                showError(e, chartExpr.getName());
            }
        }
    }

}
