package testGit.util.Notifications;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import testGit.pojo.Config;

public class Notifier {
    public static void info(String title, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("TestGit Notifications")
                .createNotification(title, message, NotificationType.INFORMATION)
                .notify(Config.getProject());
    }

    public static void warn(String title, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("TestGit Notifications")
                .createNotification(title, message, NotificationType.WARNING)
                .notify(Config.getProject());
    }

    public static void error(String title, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("TestGit Notifications")
                .createNotification(title, message, NotificationType.ERROR)
                .notify(Config.getProject());
    }
}
