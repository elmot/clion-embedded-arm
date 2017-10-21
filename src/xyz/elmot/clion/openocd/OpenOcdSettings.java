package xyz.elmot.clion.openocd;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.PathExecLazyValue;
import com.intellij.ui.components.JBCheckBox;
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
import java.util.Objects;

import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER;
import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_NONE;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW;

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
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        return !(Objects.equals(panel.boardConfigFile.getText(), state.boardConfigFile) &&
                Objects.equals(panel.openOcdLocation.getText(), state.openOcdLocation) &&
                Objects.equals(panel.gdbLocation.getText(), state.gdbLocation) &&
                Objects.equals(panel.gdbPort.getValue(), state.gdbPort) &&
                Objects.equals(panel.openOcdScriptsLocation.getText(), state.openOcdScriptsLocation) &&
                Objects.equals(panel.defaultOpenOcdScriptsLocation.isSelected(), state.defaultOpenOcdScriptsLocation));
    }

    @Override
    public void apply() throws ConfigurationException {
        panel.gdbPort.validateContent();
        File projectFile = project.getBasePath() == null ? null : new File(project.getBasePath());
        File ocdFile = checkFileExistenceAndCorrect(projectFile, panel.openOcdLocation, true);
        checkFileExistenceAndCorrect(projectFile, panel.gdbLocation, true);
        checkFileExistenceAndCorrect(openOcdDefScriptsLocation(ocdFile), panel.boardConfigFile, false);
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        state.gdbPort = panel.gdbPort.getValue();
        state.boardConfigFile = panel.boardConfigFile.getText().trim();
        state.openOcdLocation = panel.openOcdLocation.getText().trim();
        state.gdbLocation = panel.gdbLocation.getText().trim();
        state.defaultOpenOcdScriptsLocation = panel.defaultOpenOcdScriptsLocation.isSelected();
        state.openOcdScriptsLocation = panel.openOcdScriptsLocation.getText().trim();
    }

    @NotNull
    static File openOcdDefScriptsLocation(File ocdFile) {
        return new File(ocdFile.getParentFile().getParentFile(), "share/openocd/scripts");
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
        panel.openOcdScriptsLocation.setText(state.openOcdScriptsLocation);
        panel.defaultOpenOcdScriptsLocation.setSelected(state.defaultOpenOcdScriptsLocation);
        panel.openOcdScriptsLocation.setEnabled(!state.defaultOpenOcdScriptsLocation);
        panel.gdbLocation.setText(state.gdbLocation);
    }

    /**
     * (c) elmot on 20.10.2017.
     */
    public static class OpenOcdSettingsPanel extends JPanel {

        private final JBTextField boardConfigFile;
        private final JBTextField openOcdLocation;
        private final JBTextField openOcdScriptsLocation;
        private final JBCheckBox defaultOpenOcdScriptsLocation;
        private final JBTextField gdbLocation;
        private final IntegerField gdbPort;

        public OpenOcdSettingsPanel() {
            super(new GridLayoutManager(7, 3), true);
            ((GridLayoutManager) getLayout()).setColumnStretch(1, 10);
            openOcdLocation = addValueRow(0, "OpenOCD Location", new JBTextField(), null);

            defaultOpenOcdScriptsLocation = addValueRow(1, "OpenOCD Scripts Default Location", new JBCheckBox("default"), null);
            openOcdScriptsLocation = addValueRow(2, "OpenOCD Scripts Location", new JBTextField(), null);
            defaultOpenOcdScriptsLocation.addChangeListener(e -> openOcdScriptsLocation.setEnabled(!defaultOpenOcdScriptsLocation.isSelected()));

            boardConfigFile = addValueRow(3, "Board Config File", new JBTextField(), null);
            gdbLocation = addValueRow(4, "Debugger (arm-none-eabi-gdb) Location", new JBTextField(), null);
            gdbPort = addValueRow(5, "GDB Port", new IntegerField("GDB Port", 1024, 65353), null);
            gdbPort.setCanBeEmpty(false);
            add(new Spacer(), new GridConstraints(6, 0, 1, 1, ANCHOR_CENTER, FILL_NONE,
                    SIZEPOLICY_FIXED, SIZEPOLICY_WANT_GROW, null, null, null));
        }

        private <T extends Component> T addValueRow(int row, @NotNull String labelText, @NotNull T component, @Nullable Component additionalComponent) {
            JLabel label = new JLabel(labelText);
            add(label, new GridConstraints(row, 0, 1, 1, ANCHOR_WEST, FILL_NONE,
                    SIZEPOLICY_FIXED, SIZEPOLICY_FIXED, null, null, null));
            add(component, new GridConstraints(row, 1, 1, additionalComponent == null ? 2 : 1, ANCHOR_WEST,
                    FILL_HORIZONTAL, SIZEPOLICY_WANT_GROW, SIZEPOLICY_FIXED, null, null, null));
            if (additionalComponent != null) {
                add(component, new GridConstraints(row, 2, 1, 1, ANCHOR_WEST,
                        FILL_HORIZONTAL, SIZEPOLICY_WANT_GROW, SIZEPOLICY_FIXED, null, null, null));
            }
            label.setLabelFor(component);
            return component;
        }


    }
}
