package xyz.elmot.clion.charttool.state;

public enum ExpressionState {
    DISABLED("No Sampling", "Skip"),
    SAMPLE_ONCE("Sample once after clear data", "Once"),
    ALWAYS_REFRESH("Refresh on breakpoint", "Refresh"),
    ACCUMULATE("Keep All Series", "Accumulate");
    public final String buttonLabel;
    public final String hint;

    ExpressionState(String hint, String buttonLabel) {
        this.hint = hint;
        this.buttonLabel = buttonLabel;
    }

    @Override
    public String toString() {
        return buttonLabel;
    }
}
