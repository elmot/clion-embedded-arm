package xyz.elmot.clion.charttool.state;

import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Location {
    @NotNull
    public String fileUrl = "";
    public int lineNo;

    public Location(@NotNull XLineBreakpoint<?> breakpoint) {
        this(breakpoint.getFileUrl(), breakpoint.getLine());
    }

    public Location(@NotNull String fileUrl, int lineNo) {
        this.fileUrl = fileUrl;
        this.lineNo = lineNo;
    }

    @SuppressWarnings("unused")
    public Location() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Location location = (Location) o;
        return lineNo == location.lineNo &&
                Objects.equals(fileUrl, location.fileUrl);
    }

    @Override
    public int hashCode() {

        return Objects.hash(fileUrl, lineNo);
    }
}
