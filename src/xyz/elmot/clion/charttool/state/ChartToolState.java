package xyz.elmot.clion.charttool.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartToolState {

    public final Map<Location, LineState> locations = new HashMap<>();
    public final List<ChartExpr> exprs = new ArrayList<>();

}
