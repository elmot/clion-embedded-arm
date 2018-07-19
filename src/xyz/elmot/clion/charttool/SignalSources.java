package xyz.elmot.clion.charttool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerManagerListener;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.Nullable;
import xyz.elmot.clion.charttool.ui.BreakpointList;
import xyz.elmot.clion.charttool.ui.ExpressionList;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignalSources extends JBSplitter implements XDebuggerManagerListener {

    private final Project project;
    private final BreakpointList bpList;
    private final DebugListener debugListener;

    public SignalSources(Project project, DebugListener debugListener, ChartToolPersistence persistence,
                         ChartsPanel chartsPanel) {
        super(true, 0.5f, 0.1f, 0.9f);
        setBorder(JBUI.Borders.empty(15));
        this.project = project;
        this.debugListener = debugListener;
        ChartToolPersistence persistence1 = persistence;
        persistence1.setChangeListener(this::setAllBreakpoints);
        bpList = new BreakpointList(persistence);
        setAllBreakpoints();
        ExpressionList expressionList = new ExpressionList(persistence,
                () -> chartsPanel.refreshData(persistence.getExprs()));

        expressionList.setBorder(IdeBorderFactory.createTitledBorder("Expressions"));
        JBPanel<JBPanel> linesPanel = new JBPanel<>(new BorderLayout());
        linesPanel.add(bpList, BorderLayout.CENTER);
        linesPanel.add(bpList.getTableHeader(), BorderLayout.NORTH);
        linesPanel.setBorder(IdeBorderFactory.createTitledBorder("Breakpoints"));
        setFirstComponent(linesPanel);
        setSecondComponent(expressionList);
        XDebuggerManager.getInstance(project).getBreakpointManager().addBreakpointListener(bpList);

        invalidate();
    }

    public static List<XLineBreakpoint<?>> getAllXLineBreakpoints(Project project) {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        XBreakpoint<?>[] xBreakpoints = ApplicationManager.getApplication()
                .runReadAction((Computable<XBreakpoint<?>[]>) breakpointManager::getAllBreakpoints);
        return Stream.of(xBreakpoints)
                .filter(bp -> bp instanceof XLineBreakpoint)
                .map(bp -> (XLineBreakpoint<?>) bp)
                .collect(Collectors.toList());
    }

    protected void setAllBreakpoints() {
        bpList.setAllBreakpoints(getAllXLineBreakpoints(project));
    }

    @Override
    public void currentSessionChanged(@Nullable XDebugSession previousSession, @Nullable XDebugSession currentSession) {
        if (previousSession != null) {
            previousSession.removeSessionListener(debugListener);
        }
        if (currentSession != null) {
            currentSession.addSessionListener(debugListener);
        } else {
            bpList.setAllBreakpoints(null);
        }
    }


}
