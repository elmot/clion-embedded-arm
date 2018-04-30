package xyz.elmot.clion.openocd;

import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.GridBag;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationSettingsEditor;
import com.jetbrains.cidr.cpp.execution.CMakeBuildConfigurationHelper;
import org.jdesktop.swingx.JXRadioGroup;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.openocd.OpenOcdConfiguration.DownloadType;
import xyz.elmot.clion.openocd.OpenOcdConfiguration.ResetType;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class OpenOcdConfigurationEditor extends CMakeAppRunConfigurationSettingsEditor {
    private IntegerField gdbPort;
    private IntegerField telnetPort;

    private FileChooseInput boardConfigFile;
    private String openocdHome;
    private JXRadioGroup<DownloadType> downloadGroup;
    private JXRadioGroup<ResetType> resetGroup;
    private ExpandableTextField customScriptPane;

    @SuppressWarnings("WeakerAccess")
    public OpenOcdConfigurationEditor(Project project, @NotNull CMakeBuildConfigurationHelper cMakeBuildConfigurationHelper) {
        super(project, cMakeBuildConfigurationHelper);
    }

    @Override
    protected void applyEditorTo(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) throws ConfigurationException {
        super.applyEditorTo(cMakeAppRunConfiguration);

        OpenOcdConfiguration ocdConfiguration = (OpenOcdConfiguration) cMakeAppRunConfiguration;

        String boardConfig = boardConfigFile.getText().trim();
        ocdConfiguration.setBoardConfigFile(boardConfig.isEmpty() ? null : boardConfig);

        gdbPort.validateContent();
        telnetPort.validateContent();
        ocdConfiguration.setGdbPort(gdbPort.getValue());
        ocdConfiguration.setTelnetPort(telnetPort.getValue());
        ocdConfiguration.setDownloadType(downloadGroup.getSelectedValue());
        ocdConfiguration.setResetType(resetGroup.getSelectedValue());
        ocdConfiguration.setCustomScriptTemplate(customScriptPane.getText());
    }

    @Override
    protected void resetEditorFrom(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) {
        super.resetEditorFrom(cMakeAppRunConfiguration);

        OpenOcdConfiguration ocd = (OpenOcdConfiguration) cMakeAppRunConfiguration;

        openocdHome = ocd.getProject().getComponent(OpenOcdSettingsState.class).openOcdHome;

        boardConfigFile.setText(ocd.getBoardConfigFile());


        gdbPort.setText("" + ocd.getGdbPort());

        telnetPort.setText("" + ocd.getTelnetPort());
        downloadGroup.setSelectedValue(ocd.getDownloadType());
        resetGroup.setSelectedValue(ocd.getResetType());
        customScriptPane.setText(ocd.getCustomScriptTemplate());
    }

    @Override
    protected void createEditorInner(JPanel panel, GridBag gridBag) {
        super.createEditorInner(panel, gridBag);

        for (Component component : panel.getComponents()) {
            if (component instanceof CommonProgramParametersPanel) {
                component.setVisible(false);//todo get rid of this hack
            }
        }

        panel.add(new JLabel("Board config file"), gridBag.nextLine().next());
        boardConfigFile = new FileChooseInput.BoardCfg("Board config", VfsUtil.getUserHomeDir(), this::getOpenocdHome);
        panel.add(boardConfigFile, gridBag.next().coverLine());

        JPanel portsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        gdbPort = addPortInput(portsPanel, "GDB port", OpenOcdConfiguration.DEF_GDB_PORT);
        portsPanel.add(Box.createHorizontalStrut(10));

        telnetPort = addPortInput(portsPanel, "Telnet port", OpenOcdConfiguration.DEF_TELNET_PORT);

        panel.add(portsPanel, gridBag.nextLine().next().coverLine());

        panel.add(new JLabel("Download:"), gridBag.nextLine().next());
        downloadGroup = new JXRadioGroup<>(DownloadType.values());
        panel.add(downloadGroup, gridBag.next().fillCellHorizontally());
        panel.add(new JLabel("Reset:"), gridBag.nextLine().next());
        resetGroup = new JXRadioGroup<>(ResetType.values());
        panel.add(resetGroup, gridBag.next());
        resetGroup.addActionListener(event ->
                customScriptPane.setEnabled(Objects.equals(resetGroup.getSelectedValue(), ResetType.CUSTOM_SCRIPT))
        );
        customScriptPane = new ExpandableTextField();
        panel.add(new JLabel("Custom Script:"),gridBag.nextLine().next());
        panel.add(customScriptPane,gridBag.next());
    }

    private IntegerField addPortInput(JPanel portsPanel, String label, int defaultValue) {
        portsPanel.add(new JLabel(label + ": "));
        IntegerField field = new IntegerField(label, 1024, 65535);
        field.setDefaultValue(defaultValue);
        field.setColumns(5);
        portsPanel.add(field);
        return field;
    }


    private String getOpenocdHome() {
        return openocdHome;
    }

}
