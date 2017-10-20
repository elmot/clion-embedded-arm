package xyz.elmot.clion.openocd;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

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
        Notifications.Bus.notify(new Notification(GROUP_DISPLAY, FIRMWARE_DOWNLOAD, "Firmware Downloaded", NotificationType.INFORMATION), project);
    }

    @SuppressWarnings("WeakerAccess")
    public static void showFailedDownloadNotification(Project project) {
        Notifications.Bus.notify(new Notification(GROUP_DISPLAY, FIRMWARE_DOWNLOAD, "Download Failed", NotificationType.ERROR), project);
    }
}
