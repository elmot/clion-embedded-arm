package xyz.elmot.clion.openocd;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import org.jetbrains.annotations.Nullable;

/**
 * (c) elmot on 21.10.2017.
 */
@SuppressWarnings("WeakerAccess")
@State(name = "elmot.OpenOcdPlugin",storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.PER_OS))
public class OpenOcdSettingsState implements PersistentStateComponent<OpenOcdSettingsState>{
    public OpenOcdSettingsState() {

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
    }

    @Override
    public void noStateLoaded() {
        boardConfigFile = "board/stm32l4discovery.cfg";
        openOcdLocation = "/usr/local/bin/openocd";
        gdbLocation = "/usr/bin/arm-none-eabi-gdb";
        gdbPort = 3333;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpenOcdSettingsState state = (OpenOcdSettingsState) o;

        if (gdbPort != state.gdbPort) return false;
        if (boardConfigFile != null ? !boardConfigFile.equals(state.boardConfigFile) : state.boardConfigFile != null)
            return false;
        if (openOcdLocation != null ? !openOcdLocation.equals(state.openOcdLocation) : state.openOcdLocation != null)
            return false;
        return gdbLocation != null ? gdbLocation.equals(state.gdbLocation) : state.gdbLocation == null;
    }

    @Override
    public int hashCode() {
        int result = boardConfigFile != null ? boardConfigFile.hashCode() : 0;
        result = 31 * result + (openOcdLocation != null ? openOcdLocation.hashCode() : 0);
        result = 31 * result + (gdbLocation != null ? gdbLocation.hashCode() : 0);
        result = 31 * result + gdbPort;
        return result;
    }

    public String boardConfigFile;
    public String openOcdLocation;
    public String gdbLocation;
    public int gdbPort;
}
