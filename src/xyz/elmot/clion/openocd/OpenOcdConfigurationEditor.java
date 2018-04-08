package xyz.elmot.clion.openocd;

import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.GridBag;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationSettingsEditor;
import com.jetbrains.cidr.cpp.execution.CMakeBuildConfigurationHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class OpenOcdConfigurationEditor extends CMakeAppRunConfigurationSettingsEditor {
    private IntegerField gdbPort;
    private IntegerField telnetPort;

    private FileChooseInput boardConfigFile;
    private String openocdHome;

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
    }

    @Override
    protected void resetEditorFrom(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) {
        super.resetEditorFrom(cMakeAppRunConfiguration);

        OpenOcdConfiguration ocd = (OpenOcdConfiguration) cMakeAppRunConfiguration;

        openocdHome = ocd.getProject().getComponent(OpenOcdSettingsState.class).openOcdHome;

        boardConfigFile.setText(ocd.getBoardConfigFile());


        gdbPort.setText("" + ocd.getGdbPort());

        telnetPort.setText(""  + ocd.getTelnetPort());
    }

    @Override
    protected void createEditorInner(JPanel panel, GridBag gridBag) {
        super.createEditorInner(panel, gridBag);

        for (Component component: panel.getComponents()) {
            if(component instanceof CommonProgramParametersPanel) {
                component.setVisible(false);//todo get rid of this hack
            }
        }

        panel.add(new JLabel("Board config file"), gridBag.nextLine().next());
        boardConfigFile = new FileChooseInput.BoardCfg("Board config", VfsUtil.getUserHomeDir(), this::getOpenocdHome);
        panel.add(boardConfigFile, gridBag.next().coverLine());

        ((JBTextField) boardConfigFile.getChildComponent()).getEmptyText().setText("<use project default>");

        JPanel portsPanel = new JPanel();
        portsPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        gdbPort = addPortInput(portsPanel, "GDB port", OpenOcdConfiguration.DEF_GDB_PORT);
        portsPanel.add(Box.createHorizontalStrut(10));

        telnetPort = addPortInput(portsPanel,"Telnet port",OpenOcdConfiguration.DEF_TELNET_PORT);

        panel.add(portsPanel, gridBag.nextLine().next().coverLine());

    }

    private IntegerField addPortInput(JPanel portsPanel, String label, int defaultValue) {
        portsPanel.add(new JLabel(label + ": "));
        IntegerField field = new IntegerField(label, 1024,65535);
        field.setDefaultValue(defaultValue);
        field.setColumns(5);
        portsPanel.add(field);
        return field;
    }


    private String getOpenocdHome() {
        return openocdHome;
    }

}
