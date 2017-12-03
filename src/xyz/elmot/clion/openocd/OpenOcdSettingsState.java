package xyz.elmot.clion.openocd;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.components.*;
import com.intellij.openapi.util.SystemInfo;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * (c) elmot on 21.10.2017.
 */
@SuppressWarnings("WeakerAccess")
@State(name = "elmot.OpenOcdPlugin", storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.PER_OS))
public class OpenOcdSettingsState implements PersistentStateComponent<OpenOcdSettingsState> {

    public static final int DEF_GDB_PORT = 3333;
    public static final int DEF_TELNET_PORT = 4444;
    public String boardConfigFile;
    public String openOcdHome;
    public String gdbLocation;
    public boolean shippedGdb;
    public int gdbPort;
    public int telnetPort;

    public OpenOcdSettingsState() {
        boardConfigFile = "";
        openOcdHome = "/usr";
        gdbLocation = "arm-none-eabi-gdb";
        shippedGdb = true;
        gdbPort = DEF_GDB_PORT;
        telnetPort = DEF_TELNET_PORT;
    }

    @Nullable
    @Override
    public OpenOcdSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(OpenOcdSettingsState state) {
        openOcdHome = state.openOcdHome;
        boardConfigFile = state.boardConfigFile;
        gdbLocation = state.gdbLocation;
        gdbPort = state.gdbPort;
        telnetPort = state.telnetPort;
        shippedGdb = state.shippedGdb;
    }

    @Override
    public void noStateLoaded() {
        openOcdHome = OS.isWindows() ? "C:\\" : "/usr";
        File openocd = findExecutableInPath("openocd");
        if (openocd != null) {
            File folder = openocd.getParentFile();
            if (folder != null) {
                folder = folder.getParentFile();
                if (folder != null) {
                    openOcdHome = folder.getAbsolutePath();
                }
            }
        }
        File gdb = findExecutableInPath("arm-none-eabi-gdb");
        if (gdb != null) {
            gdbLocation = gdb.getAbsolutePath();
        }
    }

    @Nullable
    private File findExecutableInPath(String name) {
        if (SystemInfo.isWindows) {
            for (String ext : PathEnvironmentVariableUtil.getWindowsExecutableFileExtensions()) {
                File file = PathEnvironmentVariableUtil.findInPath(name + ext);
                if (file != null) return null;
            }
            return null;
        } else {
            return PathEnvironmentVariableUtil.findInPath(name);
        }
    }
}
