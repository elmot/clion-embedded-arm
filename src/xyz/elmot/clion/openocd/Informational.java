package xyz.elmot.clion.openocd;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;

/**
 * (c) elmot on 20.10.2017.
 */
@SuppressWarnings("WeakerAccess")
public class Informational {
    private static final String GROUP_DISPLAY = "OpenOCD";
    private static final String FIRMWARE_DOWNLOAD = "Firmware download";

    private Informational() {
    }

    @SuppressWarnings("WeakerAccess")
    public static void showSuccessfulDownloadNotification(Project project) {
        showMessage(project, MessageType.INFO, "Firmware Download Success");
    }

    private static void showMessage(Project project, MessageType messageType, String message) {
        ApplicationManager.getApplication().invokeLater(() ->
                {
                    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                    if (toolWindowManager.canShowNotification(ToolWindowId.RUN)) {
                        toolWindowManager.notifyByBalloon(ToolWindowId.RUN, messageType, message);
                    }
                }
        );
    }

    @SuppressWarnings("WeakerAccess")
    public static void showFailedDownloadNotification(Project project) {
        showMessage(project, MessageType.ERROR, "MCU Communication FAILURE");
    }
}
