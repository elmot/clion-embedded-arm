package xyz.elmot.clion.openocd;

import com.intellij.openapi.components.ProjectComponent;
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

import static com.intellij.uiDesigner.core.GridConstraints.*;

/**
 * (c) elmot on 20.10.2017.
 */
@SuppressWarnings("WeakerAccess")

public class OpenOcdSettings implements ProjectComponent, Configurable {
    //todo file choosers
    private final Project project;
    private OpenOcdSettingsPanel panel = null;

    public OpenOcdSettings(Project project) {
        this.project = project;
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
        File projectFile = project.getBasePath() == null ? null : new File(project.getBasePath());
        File ocdFile = checkFileExistenceAndCorrect(projectFile, panel.openOcdLocation, true);
        checkFileExistenceAndCorrect(projectFile, panel.gdbLocation, true);
        checkFileExistenceAndCorrect(new File(ocdFile.getParentFile().getParentFile(), "share/openocd/scripts"), panel.boardConfigFile, false);
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        state.gdbPort = panel.gdbPort.getValue();
        state.boardConfigFile = panel.boardConfigFile.getText();
        state.openOcdLocation = panel.openOcdLocation.getText();
        state.gdbLocation = panel.gdbLocation.getText();
    }

    private File checkFileExistenceAndCorrect(@Nullable File basePath, JBTextField path, boolean checkExecutable) throws ConfigurationException {
        String text = path.getText();
        File file = new File(text);
        if (!file.isAbsolute()) {
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
        //todo optional openocd parameters
        if (basePath != null) {
            URI relativized = basePath.toURI().relativize(file.toURI());
            if (relativized.isAbsolute()) {
                path.setText(file.getAbsolutePath());
            }
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
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
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
