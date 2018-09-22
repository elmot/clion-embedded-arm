package xyz.elmot.clion.openocd;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.components.*;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static xyz.elmot.clion.openocd.OpenOcdComponent.SCRIPTS_PATH_LONG;
import static xyz.elmot.clion.openocd.OpenOcdComponent.SCRIPTS_PATH_SHORT;

/**
 * (c) elmot on 21.10.2017.
 */
@SuppressWarnings("WeakerAccess")
@State(name = "elmot.OpenOcdPlugin")
public class OpenOcdSettingsState implements PersistentStateComponent<OpenOcdSettingsState> {

    public String openOcdHome;
    public boolean shippedGdb;
    public boolean autoUpdateCmake;

    public OpenOcdSettingsState() {
        openOcdHome = defOpenOcdLocation();
        shippedGdb = true;
        autoUpdateCmake = false;
    }

    public static VirtualFile findOcdScripts(VirtualFile ocdHomeVFile) {
        VirtualFile ocdScripts = null;
        if (ocdHomeVFile != null) {
            ocdScripts = ocdHomeVFile.findFileByRelativePath(SCRIPTS_PATH_LONG);
            if (ocdScripts == null) {
                ocdScripts = ocdHomeVFile.findFileByRelativePath(SCRIPTS_PATH_SHORT);
            }
        }
        return ocdScripts;
    }

    @Nullable
    @Override
    public OpenOcdSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull OpenOcdSettingsState state) {
        openOcdHome = state.openOcdHome;
        shippedGdb = state.shippedGdb;
        autoUpdateCmake = state.autoUpdateCmake;
    }

    @Override
    public void noStateLoaded() {
        openOcdHome = defOpenOcdLocation();
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
    }

    @NotNull
    protected String defOpenOcdLocation() {

        if(! OS.isWindows()) return "/usr";
        VirtualFile defDir = VfsUtil.getUserHomeDir();
        if(defDir != null) {
            return defDir.getPath();
        }
        return "C:\\";
    }

    @SuppressWarnings("SameParameterValue")
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
