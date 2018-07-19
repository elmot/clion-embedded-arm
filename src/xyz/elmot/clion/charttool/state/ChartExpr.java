package xyz.elmot.clion.charttool.state;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChartExpr {

    @NotNull
    private String expression = "";

    @Nullable
    private String name = "";

    private double xScale = 1;
    private double yScale = 1;

    private double xBase = 0;
    private double yBase = 0;

    @NotNull
    private ExpressionState state = ExpressionState.SAMPLE_ONCE;

    @Transient
    @NotNull
    private String expressionTrim = expression;

    public String getName() {

        return (name == null || "".equals(name)) ? expression : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(@NotNull String expression) {
        this.expression = expression;
        expressionTrim = expression.trim();
    }

    @NotNull
    public String getExpressionTrim() {
        return expressionTrim;
    }

    @NotNull
    public ExpressionState getState() {
        return state;
    }

    public void setState(@NotNull ExpressionState state) {
        this.state = state;
    }

    public double getXScale() {
        return xScale;
    }

    public void setXScale(double xScale) {
        this.xScale = xScale;
    }

    public double getYScale() {
        return yScale;
    }

    public void setYScale(double yScale) {
        this.yScale = yScale;
    }

    public double getXBase() {
        return xBase;
    }

    public void setXBase(double xBase) {
        this.xBase = xBase;
    }

    public double getYBase() {
        return yBase;
    }

    public void setYBase(double yBase) {
        this.yBase = yBase;
    }
}
