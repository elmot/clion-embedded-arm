package xyz.elmot.clion.openocd;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
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
    public static final int DEF_GDB_PORT = 3333;
    public static final int DEF_TELNET_PORT = 4444;
    public static final Namespace NAMESPACE = Namespace.getNamespace("elmot-ocd", "https://github.com/elmot/clion-embedded-arm");
    private static final String ATTR_GDB_PORT = "gdb-port";
    private static final String ATTR_TELNET_PORT = "telnet-port";
    private static final String ATTR_BOARD_CONFIG = "board-config";
    public static final String ATTR_RESET_TYPE = "reset-type";
    public static final String ATTR_DOWNLOAD_TYPE = "download-type";
    public static final ResetType DEFAULT_RESET = ResetType.INIT;
    private int gdbPort = DEF_GDB_PORT;
    private int telnetPort = DEF_TELNET_PORT;
    private String boardConfigFile;
    private DownloadType downloadType = DownloadType.ALWAYS;
    private ResetType resetType = DEFAULT_RESET;

    public enum DownloadType {

        ALWAYS,
        UPDATED_ONLY,
        NONE;

        @Override
        public String toString() {
            return toBeautyString(super.toString());
        }
    }

    public static String toBeautyString(String obj) {
        return StringUtil.toTitleCase(obj.toLowerCase().replace("_", " "));
    }

    public enum ResetType {
        RUN("init;reset run;"),
        INIT("init;reset init;"),
        HALT("init;reset halt"),
        NONE("");

        @Override
        public String toString() {
            return toBeautyString(super.toString());
        }

        ResetType(String command) {
            this.command = command;
        }

        private final String command;

        public final String getCommand() {
            return command;
        }

        ;

    }


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
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        boardConfigFile = element.getAttributeValue(ATTR_BOARD_CONFIG, NAMESPACE);
        gdbPort = readIntAttr(element, ATTR_GDB_PORT, DEF_GDB_PORT);
        telnetPort = readIntAttr(element, ATTR_TELNET_PORT, DEF_TELNET_PORT);
        resetType = readEnumAttr(element, ATTR_RESET_TYPE, DEFAULT_RESET);
        downloadType = readEnumAttr(element, ATTR_DOWNLOAD_TYPE, DownloadType.ALWAYS);
    }

    private int readIntAttr(@NotNull Element element, String name, int def) {
        String s = element.getAttributeValue(name, NAMESPACE);
        if (StringUtil.isEmpty(s)) return def;
        return Integer.parseUnsignedInt(s);
    }

    private <T extends Enum> T readEnumAttr(@NotNull Element element, String name, T def) {
        String s = element.getAttributeValue(name, NAMESPACE);
        if (StringUtil.isEmpty(s)) return def;
        try {
            //noinspection unchecked
            return (T) Enum.valueOf(def.getDeclaringClass(), s);
        } catch (Throwable t) {
            return def;
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        element.setAttribute(ATTR_GDB_PORT, "" + gdbPort, NAMESPACE);
        element.setAttribute(ATTR_TELNET_PORT, "" + telnetPort, NAMESPACE);
        if (boardConfigFile != null) {
            element.setAttribute(ATTR_BOARD_CONFIG, boardConfigFile, NAMESPACE);
        }
        element.setAttribute(ATTR_RESET_TYPE, resetType.name(), NAMESPACE);
        element.setAttribute(ATTR_DOWNLOAD_TYPE, downloadType.name(), NAMESPACE);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        super.checkConfiguration();
        checkPort(gdbPort);
        checkPort(telnetPort);
        if (gdbPort == telnetPort) {
            throw new RuntimeConfigurationException("Port values should be different");
        }
        if (StringUtil.isEmpty(boardConfigFile)) {
            throw new RuntimeConfigurationException("Board config file is not defined");
        }
    }

    private void checkPort(int port) throws RuntimeConfigurationException {
        if (port <= 1024 || port > 65535)
            throw new RuntimeConfigurationException("Port value must be in the range [1024...65535]");
    }

    public int getGdbPort() {
        return gdbPort;
    }

    public void setGdbPort(int gdbPort) {
        this.gdbPort = gdbPort;
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    public String getBoardConfigFile() {
        return boardConfigFile;
    }

    public void setBoardConfigFile(String boardConfigFile) {
        this.boardConfigFile = boardConfigFile;
    }

    public DownloadType getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(DownloadType downloadType) {
        this.downloadType = downloadType;
    }

    public ResetType getResetType() {
        return resetType;
    }

    public void setResetType(ResetType resetType) {
        this.resetType = resetType;
    }
}
