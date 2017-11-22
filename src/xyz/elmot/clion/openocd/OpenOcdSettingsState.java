package xyz.elmot.clion.openocd;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.EnvironmentUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.StringTokenizer;

/**
 * (c) elmot on 21.10.2017.
 */
@SuppressWarnings("WeakerAccess")
@State(name = "elmot.OpenOcdPlugin", storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.PER_OS))
public class OpenOcdSettingsState implements PersistentStateComponent<OpenOcdSettingsState> {

    public static final int DEF_GDB_PORT = 3333;
    public static final int DEF_TELNET_PORT = 4444;

    public OpenOcdSettingsState() {
        boardConfigFile = "";
        openOcdLocation = "/usr/bin/openocd";
        gdbLocation = "arm-none-eabi-gdb";
        defaultOpenOcdScriptsLocation = true;
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
        boardConfigFile = state.boardConfigFile;
        openOcdLocation = state.openOcdLocation;
        gdbLocation = state.gdbLocation;
        gdbPort = state.gdbPort;
        telnetPort = state.telnetPort;
        openOcdScriptsLocation = state.openOcdScriptsLocation;
        defaultOpenOcdScriptsLocation = state.defaultOpenOcdScriptsLocation;
        shippedGdb = state.shippedGdb;
    }

    @Override
    public void noStateLoaded() {
        String path = EnvironmentUtil.getValue("PATH");
        if (path == null) return;
        File openocd = findExecutableInPath(path, "openocd");
        if (openocd != null) {
            openOcdLocation = openocd.getAbsolutePath();
            openOcdScriptsLocation = OpenOcdSettings.openOcdDefScriptsLocation(openocd).getAbsolutePath();
        }
        File gdb = findExecutableInPath(path, "arm-none-eabi-gdb");
        if (gdb != null) {
            gdbLocation = gdb.getAbsolutePath();
        }

    }

    @Nullable
    private File findExecutableInPath(String path, String name) {
        for (StringTokenizer stringTokenizer = new StringTokenizer(path, File.pathSeparator); stringTokenizer.hasMoreTokens(); ) {
            String pathElement = stringTokenizer.nextToken();
            File pretender = new File(pathElement, name);
            if (pretender.canExecute()) {
                return pretender;
            }
        }
        return null;
    }

    public String boardConfigFile;
    public String openOcdLocation;
    public String openOcdScriptsLocation;
    public boolean defaultOpenOcdScriptsLocation;
    public String gdbLocation;
    public boolean shippedGdb;
    public int gdbPort;
    public int telnetPort;
}
