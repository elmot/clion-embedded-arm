package xyz.elmot.clion.openocd;

import com.google.common.annotations.VisibleForTesting;
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
    public static final String ATTR_UPLOAD_TYPE = "upload-type";
    public static final ResetType DEFAULT_RESET = ResetType.INIT;
    public static final UploadType DEFAULT_UPLOAD = UploadType.ALWAYS;
    public static final String ATTR_CUSTOM_RESET = "custom-reset-cmd";
    public static final String ATTR_CUSTOM_UPLOAD = "custom-upload-cmd";
    public static final String DEFAULT_UPLOAD_CMD = "program";
    private int gdbPort = DEF_GDB_PORT;
    private int telnetPort = DEF_TELNET_PORT;
    private String boardConfigFile;
    private String customResetCmd;
    private String customUploadCmd = DEFAULT_UPLOAD_CMD;

    private UploadType uploadType = DEFAULT_UPLOAD;
    private ResetType resetType = DEFAULT_RESET;

    public enum UploadType {

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
        RUN("init;reset run"),
        HALT("init;reset halt"),
        INIT("init;reset init"),
        NONE(""),
        CUSTOM("");

        private String resetCmd;

        ResetType(String cmd) {
            resetCmd = cmd;
        }

        public String getCommand() {
            return resetCmd;
        }

        @Override
        public String toString() {
            return toBeautyString(super.toString());
        }
    };

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
        boardConfigFile = readStringAttr(element, ATTR_BOARD_CONFIG, "");
        customResetCmd  = readStringAttr(element, ATTR_CUSTOM_RESET, "");
        customUploadCmd = readStringAttr(element, ATTR_CUSTOM_UPLOAD, DEFAULT_UPLOAD_CMD);
        gdbPort = readIntAttr(element, ATTR_GDB_PORT, DEF_GDB_PORT);
        telnetPort = readIntAttr(element, ATTR_TELNET_PORT, DEF_TELNET_PORT);
        resetType = readEnumAttr(element, ATTR_RESET_TYPE, DEFAULT_RESET);
        uploadType = readEnumAttr(element, ATTR_UPLOAD_TYPE, DEFAULT_UPLOAD);
    }

    @VisibleForTesting
    public static int readIntAttr(@NotNull Element element, String name, int def) {
        String s = element.getAttributeValue(name, NAMESPACE);
        if (StringUtil.isEmpty(s)) return def;
        try {
            return Integer.parseUnsignedInt(s);
        } catch(NumberFormatException e) {
            // XML data fromat mismatch; return default; TODO: log the issue
            return def;
        }
    }

    @VisibleForTesting
    public static String readStringAttr(@NotNull Element element, String name, String def) {
        String s = element.getAttributeValue(name, NAMESPACE);
        if (StringUtil.isEmpty(s)) return def;
        return s;
    }

    @VisibleForTesting
    public static <T extends Enum> T readEnumAttr(@NotNull Element element, String name, T def) {
        String s = element.getAttributeValue(name, NAMESPACE);
        if (StringUtil.isEmpty(s)) return def;
        try {
            //noinspection unchecked
            return (T) Enum.valueOf(def.getClass(), s);
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
        element.setAttribute(ATTR_UPLOAD_TYPE, uploadType.name(), NAMESPACE);
        if (resetType.equals(ResetType.CUSTOM)) {
            element.setAttribute(ATTR_CUSTOM_RESET, customResetCmd, NAMESPACE);
        }
        if (!customUploadCmd.equals(DEFAULT_UPLOAD_CMD)) {
            element.setAttribute(ATTR_CUSTOM_UPLOAD, customUploadCmd, NAMESPACE);
        }
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

    public UploadType getUploadType() {
        return uploadType;
    }

    public void setUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
    }

    public ResetType getResetType() {
        return resetType;
    }

    public void setResetType(ResetType resetType) {
        this.resetType = resetType;
    }

    public String getResetCommand() {
        return resetType.equals(ResetType.CUSTOM) ? customResetCmd : resetType.getCommand();
    }

    public void setResetCommand(String cmd) {
        if (resetType.equals(ResetType.CUSTOM)) {
            customResetCmd = cmd;
        } else {
            customResetCmd = "";
        }
    }

    public String getCustomResetCommand() {
        return customResetCmd;
    }

    public void setCustomResetCommand(String cmd) {
        this.customResetCmd = cmd;
    }

    public String getUploadCommand() {
        return customUploadCmd;
    }

    public void setUploadCommand(String cmd) {
        customUploadCmd = cmd;
    }

}
