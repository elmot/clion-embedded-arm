package xyz.elmot.clion.openocd;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Objects;

import static com.intellij.uiDesigner.core.GridConstraints.*;

/**
 * (c) elmot on 20.10.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OpenOcdSettings implements ProjectComponent, Configurable {
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
        return !(
                Objects.equals(panel.boardConfigFile.getText(), state.boardConfigFile) &&
                        Objects.equals(panel.openOcdHome.getText(), state.openOcdHome) &&
                        Objects.equals(panel.gdbLocation.getText(), state.gdbLocation) &&
                        (panel.gdbPort.getValue() == state.gdbPort) &&
                        (panel.telnetPort.getValue() == state.telnetPort) &&
                        panel.shippedGdb.isSelected() == state.shippedGdb);
    }

    @Override
    public void apply() throws ConfigurationException {
        panel.gdbPort.validateContent();
        panel.telnetPort.validateContent();
        if (!panel.shippedGdb.isSelected()) {
            panel.gdbLocation.validateContent();
        }
        panel.openOcdHome.validateContent();
        panel.boardConfigFile.validateContent();

        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        state.gdbPort = panel.gdbPort.getValue();
        state.telnetPort = panel.telnetPort.getValue();
        state.boardConfigFile = panel.boardConfigFile.getText();
        state.openOcdHome = panel.openOcdHome.getText();
        state.gdbLocation = panel.gdbLocation.getText();
        state.shippedGdb = panel.shippedGdb.isSelected();
    }

    @Override
    public void disposeComponent() {
        panel = null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        panel = new OpenOcdSettingsPanel();
        return panel;
    }

    @Override
    public void reset() {
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        panel.gdbPort.setValue(state.gdbPort);
        panel.telnetPort.setValue(state.telnetPort);
        panel.openOcdHome.setText(state.openOcdHome);
        panel.boardConfigFile.setText(state.boardConfigFile);
        panel.shippedGdb.setSelected(state.shippedGdb);
        panel.gdbLocation.setText(state.gdbLocation);
    }

    /**
     * (c) elmot on 20.10.2017.
     */
    public static class OpenOcdSettingsPanel extends JPanel {

        private final FileChooseInput boardConfigFile;
        private final FileChooseInput openOcdHome;
        private final JBCheckBox shippedGdb;
        private final FileChooseInput gdbLocation;
        private final IntegerField gdbPort;
        private final IntegerField telnetPort;

        public OpenOcdSettingsPanel() {
            super(new GridLayoutManager(8, 3), true);
            ((GridLayoutManager) getLayout()).setColumnStretch(1, 10);
            openOcdHome = addValueRow(0,  new FileChooseInput.OpenOcdHome("OpenOCD Home", VfsUtil.getUserHomeDir()));

            boardConfigFile = addValueRow(1, new FileChooseInput.BoardCfg("Board Config File",
                    VfsUtil.getUserHomeDir(), openOcdHome::getText));

            gdbPort = addValueRow(2, "OpenOCD GDB Port", new IntegerField("GDB Port", 1024, 65353));
            gdbPort.setCanBeEmpty(false);
            telnetPort = addValueRow(3, "OpenOCD Telnet Port", new IntegerField("Telnet Port", 1024, 65353));
            telnetPort.setCanBeEmpty(false);
            shippedGdb = addValueRow(5, "GDB (arm-none-eabi-gdb)", new JBCheckBox("Use shipped with CLion"));

            gdbLocation = addValueRow(6, new FileChooseInput.ExeFile("GDB Location", VfsUtil.getUserHomeDir()));

            shippedGdb.addChangeListener(e -> gdbLocation.setEnabled(!shippedGdb.isSelected()));
            add(new Spacer(), new GridConstraints(7, 0, 1, 1, ANCHOR_CENTER, FILL_NONE,
                    SIZEPOLICY_FIXED, SIZEPOLICY_WANT_GROW, null, null, null));
        }

        private <T extends FileChooseInput> T addValueRow(int row, @NotNull T component) {
            return addValueRow(row,component.getValueName(),component);
        }
        private <T extends JComponent> T addValueRow(int row, @Nullable String labelText, @NotNull T component) {
            add(component, new GridConstraints(row, 1, 1, 1, ANCHOR_WEST,
                    FILL_HORIZONTAL, SIZEPOLICY_WANT_GROW, SIZEPOLICY_FIXED, null, null, null));
            if (labelText != null) {
                JLabel label = new JLabel(labelText);
                add(label, new GridConstraints(row, 0, 1, 1, ANCHOR_WEST, FILL_NONE,
                        SIZEPOLICY_FIXED, SIZEPOLICY_FIXED, null, null, null));
                label.setLabelFor(component);
            }
            return component;
        }
    }
}
