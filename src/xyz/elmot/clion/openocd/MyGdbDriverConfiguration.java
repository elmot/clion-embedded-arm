package xyz.elmot.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.execution.debugger.backend.GDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MyGdbDriverConfiguration extends GDBDriverConfiguration {
    public MyGdbDriverConfiguration(@NotNull Project project, @Nullable CPPToolchains.Toolchain toolchain) {
        super(project, toolchain);
    }

    public MyGdbDriverConfiguration(@NotNull Project project, @Nullable CPPToolchains.Toolchain toolchain, boolean b) {
        super(project, toolchain, b);
    }

    @NotNull
    @Override
    public GeneralCommandLine createDriverCommandLine(@NotNull DebuggerDriver debuggerDriver) throws ExecutionException {
        GeneralCommandLine driverCommandLine = super.createDriverCommandLine(debuggerDriver);
        driverCommandLine.addParameters("-ex", "show configuration");
        return driverCommandLine;
    }
}
