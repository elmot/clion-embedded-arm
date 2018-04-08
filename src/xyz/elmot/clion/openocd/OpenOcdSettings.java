package xyz.elmot.clion.openocd;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Objects;

import static com.intellij.uiDesigner.core.GridConstraints.*;

/**
 * (c) elmot on 20.10.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OpenOcdSettings implements ProjectComponent, Configurable {
    public static final OpenOcdSettingsState DEFAULT = new OpenOcdSettingsState();
    protected final Project project;
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
        if (state == null) return true;
        return !(
                        Objects.equals(panel.openOcdHome.getText(), state.openOcdHome) &&
                        panel.shippedRadioButton.isSelected() == state.shippedGdb &&
                        panel.autoUpdateCmake.isSelected() == state.autoUpdateCmake);
    }

    @Override
    public void apply() throws ConfigurationException {
        panel.openOcdHome.validateContent();

        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class);
        if (state != null) {
            state.openOcdHome = panel.openOcdHome.getText();
            state.shippedGdb = panel.shippedRadioButton.isSelected();
            state.autoUpdateCmake = panel.autoUpdateCmake.isSelected();
        }
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
        OpenOcdSettingsState state = project.getComponent(OpenOcdSettingsState.class, DEFAULT);
        panel.openOcdHome.setText(state.openOcdHome);
        panel.shippedRadioButton.setSelected(state.shippedGdb);
        panel.toolchainRadioButton.setSelected(!state.shippedGdb);
        panel.updateToolchainGdbName();
        panel.autoUpdateCmake.setSelected(state.autoUpdateCmake);
    }

    /**
     * (c) elmot on 20.10.2017.
     */
    public static class OpenOcdSettingsPanel extends JPanel {

//        private final FileChooseInput boardConfigFile;
        private final FileChooseInput openOcdHome;
        private final JBCheckBox autoUpdateCmake;
        private JRadioButton toolchainRadioButton;
        private JRadioButton shippedRadioButton;

        public OpenOcdSettingsPanel() {
            super(new GridLayoutManager(6, 3), true);
            ((GridLayoutManager) getLayout()).setColumnStretch(1, 10);
            openOcdHome = addValueRow(0, new FileChooseInput.OpenOcdHome("OpenOCD Home", VfsUtil.getUserHomeDir()));

            addValueRow(2, "Use GDB", setupGdbButtonGroup());

            autoUpdateCmake = addValueRow(4, "CMake Project Update", new JBCheckBox("Automatic"));

            add(new Spacer(), new GridConstraints(5, 0, 1, 1, ANCHOR_CENTER, FILL_NONE,
                    SIZEPOLICY_FIXED, SIZEPOLICY_WANT_GROW, null, null, null));
        }

        @NotNull
        protected JPanel setupGdbButtonGroup() {
            shippedRadioButton = new JRadioButton("Shipped with CLion");

            toolchainRadioButton = new JRadioButton();
            updateToolchainGdbName();
            ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(shippedRadioButton);
            buttonGroup.add(toolchainRadioButton);
            JPanel gdbPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

            gdbPanel.add(shippedRadioButton);
            gdbPanel.add(toolchainRadioButton);
            return gdbPanel;
        }

        private void updateToolchainGdbName() {
            CPPToolchains.Toolchain toolchain = CPPToolchains.getInstance().getDefaultToolchain();
            File debugger = toolchain == null ? null: toolchain.getDebugger().getGdbExecutable();
            if(debugger == null){
                toolchainRadioButton.setText("From Toolchain");
                toolchainRadioButton.setToolTipText(null);
            }else {
                toolchainRadioButton.setText(String.format("From Toolchain (%s)",debugger.getName()));
                toolchainRadioButton.setToolTipText(debugger.getAbsolutePath());
            }
        }

        @SuppressWarnings("SameParameterValue")
        private <T extends FileChooseInput> T addValueRow(int row, @NotNull T component) {
            return addValueRow(row, component.getValueName(), component);
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
