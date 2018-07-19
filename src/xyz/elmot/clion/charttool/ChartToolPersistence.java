package xyz.elmot.clion.charttool;

import com.intellij.openapi.components.PersistentStateComponentWithModificationTracker;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.elmot.clion.charttool.state.ChartExpr;
import xyz.elmot.clion.charttool.state.ChartToolState;
import xyz.elmot.clion.charttool.state.LineState;
import xyz.elmot.clion.charttool.state.Location;

import java.util.ArrayList;
import java.util.List;

import static xyz.elmot.clion.charttool.ChartTool.CHART_EXPR_KEY;

@State(name = "charttool")
public class ChartToolPersistence implements PersistentStateComponentWithModificationTracker<ChartToolState> {
    private final Project project;
    private long modificationsCount = 0;
    private Runnable changeListener;
    private final List<ChartExpr> exprs = new ArrayList<>();

    public ChartToolPersistence(Project project) {
        this.project = project;
    }

    @Override
    public long getStateModificationCount() {
        return modificationsCount;
    }

    @Nullable
    @Override
    public ChartToolState getState() {
        ChartToolState state = new ChartToolState();

        for (XLineBreakpoint<?> breakpoint : SignalSources.getAllXLineBreakpoints(project)) {
            LineState lineState = breakpoint.getUserData(CHART_EXPR_KEY);

            if (lineState != null) {
                Location location = new Location(breakpoint);
                state.locations.put(location, lineState);
            }
        }
        state.exprs.clear();
        state.exprs.addAll(exprs);
        return state;
    }

    @Override
    public void loadState(@NotNull ChartToolState state) {

        for (XLineBreakpoint<?> breakpoint : SignalSources.getAllXLineBreakpoints(project)) {
            LineState lineState = state.locations.get(new Location(breakpoint));
            breakpoint.putUserData(CHART_EXPR_KEY, lineState);
        }
        exprs.clear();
        exprs.addAll(state.exprs);

        if (changeListener != null) {
            changeListener.run();
        }

    }

    public void registerChange() {
        modificationsCount++;
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    public List<ChartExpr> getExprs() {
        return exprs;
    }
}
