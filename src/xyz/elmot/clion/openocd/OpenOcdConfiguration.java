package xyz.elmot.clion.openocd;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.execution.CidrCommandLineState;
import com.jetbrains.cidr.execution.CidrExecutableDataHolder;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * (c) elmot on 29.9.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OpenOcdConfiguration extends CMakeAppRunConfiguration implements CidrExecutableDataHolder {
    public static final Namespace NAMESPACE = Namespace.getNamespace("ocd", "https://github.com/elmot/clion-embedded-arm/xmlns");
    public static final String BOARD_FILE = "board-file";
    public static final String GDB_PORT = "gdb-port";
    public static final String TELNET_PORT = "telnet-port";

    public static final int NO_PORT = -1;

    @Nullable
    private String boardFile = null;

    private int gdbPort = NO_PORT;
    private int telnetPort = NO_PORT;

    @SuppressWarnings("WeakerAccess")
    public OpenOcdConfiguration(Project project, ConfigurationFactory configurationFactory, String targetName) {
        super(project, configurationFactory, targetName);
    }

    @Nullable
    @Override
    public CidrCommandLineState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new CidrCommandLineState(environment, new OpenOcdLauncher(this));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        if (boardFile != null) {
            element.setAttribute(BOARD_FILE, boardFile, NAMESPACE);
        }

        if (gdbPort != NO_PORT) {
            element.setAttribute(GDB_PORT, String.valueOf(gdbPort), NAMESPACE);
        }

        if (telnetPort != NO_PORT) {
            element.setAttribute(TELNET_PORT, String.valueOf(telnetPort), NAMESPACE);
        }
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        boardFile = element.getAttributeValue(BOARD_FILE, NAMESPACE, null);

        String gdbPort = element.getAttributeValue(GDB_PORT, NAMESPACE);
        this.gdbPort = gdbPort != null ? Integer.parseInt(gdbPort) : NO_PORT;

        String telnetPort = element.getAttributeValue(TELNET_PORT, NAMESPACE);
        this.telnetPort = telnetPort != null ? Integer.parseInt(telnetPort) : NO_PORT;
    }

    @Nullable
    public String getBoardFile() {
        return boardFile;
    }

    public void setBoardFile(@Nullable String boardFile) {
        this.boardFile = boardFile;
    }

    public boolean hasBoardFile() {
        return boardFile != null;
    }

    public int getGdbPort() {
        return gdbPort;
    }

    public void setGdbPort(int gdbPort) {
        this.gdbPort = gdbPort;
    }

    public boolean hasGdbPort() {
        return gdbPort != NO_PORT;
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    public boolean hasTelnetPort() {
        return telnetPort != NO_PORT;
    }

    public static String actualBoardFile(@Nullable OpenOcdConfiguration configuration,
                                         @NotNull OpenOcdSettingsState settings) {
        return configuration != null && configuration.hasBoardFile() ?
                configuration.getBoardFile() : settings.boardConfigFile;
    }

    public static int actualGdbPort(@Nullable OpenOcdConfiguration configuration,
                                    @NotNull OpenOcdSettingsState settings) {
        return configuration != null && configuration.hasGdbPort() ?
                configuration.getGdbPort() : settings.gdbPort;
    }

    public static int actualTelnetPort(@Nullable OpenOcdConfiguration configuration,
                                       @NotNull OpenOcdSettingsState settings) {
        return configuration != null && configuration.hasTelnetPort() ?
                configuration.getTelnetPort() : settings.telnetPort;
    }
}
