package xyz.elmot.clion.openocd;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.function.Consumer;

import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER;
import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST;
import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_NONE;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW;

/**
 * (c) elmot on 20.10.2017.
 */
@State(name = "elmot.OpenOcdPlugin")
public class OpenOcdSettings implements ProjectComponent, PersistentStateComponent<OpenOcdSettings.State>, Configurable {
    //todo file choosers
    private final Project project;
    private File projectFile;

    public OpenOcdSettings(Project project) {
        this.project = project;
    }

    private State state = new State();
    private OpenOcdSettingsPanel panel = null;

    @Nullable
    @Override
    public OpenOcdSettings.State getState() {
        return this.state;
    }

    @Override
    public void loadState(OpenOcdSettings.State state) {
        this.state = state;
    }

    public static class State {
        private String boardConfigFile = "board/stm32l4discovery.cfg";
        private String openOcdLocation = "/usr/local/bin/openocd";
        private String gdbLocation = "/usr/bin/arm-none-eabi-gdb";
        private int gdbPort = 3333;
    }

    public String getBoardConfigFile() {
        return state.boardConfigFile;
    }

    public String getOpenOcdLocation() {
        return state.openOcdLocation;
    }

    public String getGdbLocation() {
        return state.gdbLocation;
    }

    public int getGdbPort() {
        return state.gdbPort;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "ARM/OpenOCD support";
    }

    @Override
    public boolean isModified() {
        return true;//todo
    }

    @Override
    public void apply() throws ConfigurationException {
        panel.gdbPort.validateContent();
        projectFile = project.getBasePath() == null ? null : new File(project.getBasePath());
        File ocdFile = checkFileExistenceAndCorrect(projectFile, panel.openOcdLocation, true);
        checkFileExistenceAndCorrect(projectFile, panel.gdbLocation, true);
        checkFileExistenceAndCorrect(new File(ocdFile.getParentFile().getParentFile(),"share/openocd/scripts"), panel.boardConfigFile, false);
        state.gdbPort = panel.gdbPort.getValue();
        state.boardConfigFile = panel.boardConfigFile.getText();
        state.openOcdLocation = panel.openOcdLocation.getText();
        state.gdbLocation = panel.gdbLocation.getText();
    }

    private File checkFileExistenceAndCorrect(@Nullable File basePath, JBTextField path, boolean checkExecutable) throws ConfigurationException {
        String text = path.getText();
        File file = new File(text);
        if(!file.isAbsolute())
        {
            file = new File(basePath, text);
        }
        if (!file.exists() || !file.isFile()) {
            throw new ConfigurationException("File " + text + " does not exist.");
        }
        if (checkExecutable) {
            if (!file.canExecute()) {
                throw new ConfigurationException("File " + text + " is not executable");
            }
        } else {
            if (!file.canRead()) {
                throw new ConfigurationException("File " + text + " is not readable");
            }

        }
        if (basePath != null) {
            URI relativized = basePath.toURI().relativize(file.toURI());
            path.setText(relativized.getRawPath());
        }
        return file;
    }

    @Override
    public void disposeComponent() {
        panel = null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        if (panel == null) panel = new OpenOcdSettingsPanel();
        return panel;
    }

    @Override
    public void reset() {
        panel.gdbPort.setValue(state.gdbPort);
        panel.boardConfigFile.setText(state.boardConfigFile);
        panel.openOcdLocation.setText(state.openOcdLocation);
        panel.gdbLocation.setText(state.gdbLocation);
    }

    /**
     * (c) elmot on 20.10.2017.
     */
    public static class OpenOcdSettingsPanel extends JPanel {

        private final IntegerField gdbPort;
        private final JBTextField boardConfigFile;
        private final JBTextField openOcdLocation;
        private final JBTextField gdbLocation;

        public OpenOcdSettingsPanel() {
            super(new GridLayoutManager(5, 3), true);

            gdbPort = addValueRow(0, "GDB Port", s(new IntegerField("GDB Port", 1024, 65353), f -> f.setCanBeEmpty(false)))[0];
            boardConfigFile = addValueRow(1, "Board Config File", new JBTextField())[0];
            openOcdLocation = addValueRow(2, "OpenOCD Location", new JBTextField())[0];
            gdbLocation = addValueRow(3, "Debugger (arm-none-eabi-gdb) Location", new JBTextField())[0];
            add(new Spacer(), new GridConstraints(4, 0, 1, 1, ANCHOR_CENTER, FILL_NONE, SIZEPOLICY_WANT_GROW, SIZEPOLICY_WANT_GROW, null, null, null));
        }

        private <T extends Component> T[] addValueRow(int row, @NotNull String labelText, @NotNull T... components) {
            JLabel label = new JLabel(labelText);
            add(label, new GridConstraints(row, 0, 1, 1, ANCHOR_EAST, FILL_NONE, SIZEPOLICY_CAN_SHRINK, SIZEPOLICY_FIXED, null, null, null));
            for (int i = 0; i < components.length; i++) {
                add(components[i], new GridConstraints(row, i + 1, 1, 1, ANCHOR_WEST, FILL_HORIZONTAL, SIZEPOLICY_WANT_GROW, SIZEPOLICY_FIXED, null, null, null));
            }
            label.setLabelFor(components[0]);
            return components;
        }

        private <T extends Component> T s/*etup*/(T component, Consumer<T> setup) {
            setup.accept(component);
            return component;
        }

    }
}
