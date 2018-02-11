package xyz.elmot.clion.openocd;

import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.GridBag;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationSettingsEditor;
import com.jetbrains.cidr.cpp.execution.CMakeBuildConfigurationHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class OpenOcdConfigurationEditor extends CMakeAppRunConfigurationSettingsEditor {
    private String openocdHome;
    private JBTextField gdbPort, telnetPort;
    private FileChooseInput boardConfigFile;

    public OpenOcdConfigurationEditor(Project project, @NotNull CMakeBuildConfigurationHelper cMakeBuildConfigurationHelper) {
        super(project, cMakeBuildConfigurationHelper);
    }

    @Override
    protected void applyEditorTo(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) throws ConfigurationException {
        super.applyEditorTo(cMakeAppRunConfiguration);

        OpenOcdConfiguration ocd = (OpenOcdConfiguration) cMakeAppRunConfiguration;

        String boardConfig = boardConfigFile.getText().trim();
        ocd.setBoardFile(boardConfig.isEmpty() ? null : boardConfig);

        ocd.setGdbPort(parsePort(gdbPort.getText()));
        ocd.setTelnetPort(parsePort(telnetPort.getText()));
    }

    @Override
    protected void resetEditorFrom(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) {
        super.resetEditorFrom(cMakeAppRunConfiguration);

        OpenOcdConfiguration ocd = (OpenOcdConfiguration) cMakeAppRunConfiguration;

        openocdHome = ocd.getProject().getComponent(OpenOcdSettingsState.class).openOcdHome;

        boardConfigFile.setText("");
        if (ocd.hasBoardFile()) {
            boardConfigFile.setText(ocd.getBoardFile());
        }

        gdbPort.setText("");
        if (ocd.hasGdbPort()) {
            gdbPort.setText(String.valueOf(ocd.getGdbPort()));
        }

        telnetPort.setText("");
        if (ocd.hasTelnetPort()) {
            telnetPort.setText(String.valueOf(ocd.getTelnetPort()));
        }
    }

    private void portField(JBTextField f) {
        f.getEmptyText().setText("<default>");

        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (parsePort(f.getText()) == OpenOcdConfiguration.NO_PORT) {
                    f.setText("");
                }
            }
        });

        Dimension pref = f.getPreferredSize();
        f.setPreferredSize(new Dimension(100, pref.height));
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
        panel.add(boardConfigFile = new FileChooseInput.BoardCfg("Board config", VfsUtil.getUserHomeDir(),
                this::getOpenocdHome), gridBag.next().coverLine());

        ((JBTextField) boardConfigFile.getChildComponent()).getEmptyText().setText("<use project default>");

        JPanel portsPanel = new JPanel();
        portsPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        portsPanel.add(new JLabel("GDB port: "));
        portsPanel.add(gdbPort = new JBTextField());
        portsPanel.add(Box.createHorizontalStrut(10));
        portsPanel.add(new JLabel("Telnet port: "));
        portsPanel.add(telnetPort = new JBTextField());

        panel.add(portsPanel, gridBag.nextLine().next().coverLine());

        portField(gdbPort);
        portField(telnetPort);
    }

    private String getOpenocdHome() {
        return openocdHome;
    }

    private static int parsePort(String text) {
        try {
            int r = Integer.parseInt(text);
            return r <= 0 || r > 65535 ? OpenOcdConfiguration.NO_PORT : r;
        } catch (NumberFormatException e) {
            return OpenOcdConfiguration.NO_PORT;
        }
    }
}
