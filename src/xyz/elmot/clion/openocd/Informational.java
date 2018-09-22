package xyz.elmot.clion.openocd;

import javax.swing.event.HyperlinkEvent;
import java.util.Objects;

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.HyperlinkAdapter;

/**
 * (c) elmot on 20.10.2017.
 */
@SuppressWarnings("WeakerAccess")
public class Informational {
    public static final String SETTINGS_PROTOCOL = "settings://";
    public static final String HELP_URL = "https://github.com/elmot/clion-embedded-arm/blob/master/USAGE.md";

    private Informational() {
    }

    @SuppressWarnings("WeakerAccess")
    public static void showSuccessfulDownloadNotification(Project project) {
        showMessage(project, MessageType.INFO, "Firmware Download Success");
    }

    public static void showMessage(Project project, MessageType messageType, String message) {
        ApplicationManager.getApplication().invokeLater(() ->
                {
                    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                    if (toolWindowManager.canShowNotification(ToolWindowId.RUN)) {
                        toolWindowManager.notifyByBalloon(ToolWindowId.RUN, messageType, message,
                                OpenOcdConfigurationType.ICON.getValue(),
                                new HyperlinkHandler(project)
                        );
                    }
                }
        );
    }

    @SuppressWarnings("WeakerAccess")
    public static void showFailedDownloadNotification(Project project) {
        showMessage(project, MessageType.ERROR,
                "MCU Communication FAILURE.\nCheck <a href=\""+
                        SETTINGS_PROTOCOL +
                        OpenOcdSettings.class.getName()
                        +"\">OpenOCD configuration</a> and connection.<br>" +
                        "Plugin documentation is located <a href=\"" + HELP_URL + "\">here</a>");
    }

    public static void showPluginError(Project project, ConfigurationException e) {
        int optionNo = Messages.showDialog(project, e.getLocalizedMessage(), e.getTitle(),
                new String[]{Messages.OK_BUTTON, CommonBundle.settingsAction(), CommonBundle.getHelpButtonText()}
                , 0, Messages.getErrorIcon());
        switch (optionNo) {
            case 1:
                ShowSettingsUtil.getInstance().showSettingsDialog(project, OpenOcdSettings.class);
                break;
            case 2:
                BrowserUtil.browse(HELP_URL);
                break;
            default://nothing to do
        }
    }

    private static class HyperlinkHandler extends HyperlinkAdapter {
        private final Project project;

        public HyperlinkHandler(Project project) {
            this.project = project;
        }

        @Override
        protected void hyperlinkActivated(HyperlinkEvent e) {
            String link = Objects.toString(e.getDescription(),"");
            if(link.toLowerCase().startsWith(SETTINGS_PROTOCOL)) {
                try {
                    String className = link.substring(SETTINGS_PROTOCOL.length());
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, Class.forName(className));
                } catch (ClassNotFoundException ignored) {
                }
            } else {
                BrowserUtil.browse(link);
            }
        }
    }
}
