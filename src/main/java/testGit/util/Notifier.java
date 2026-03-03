package testGit.util;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import testGit.pojo.Config;

public class Notifier {
    public static void information(String title, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("TestGit Notifications")
                .createNotification(title, content, NotificationType.INFORMATION)
                .notify(Config.getProject());
    }

    public static void warning(String title, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("TestGit Notifications")
                .createNotification(title, content, NotificationType.WARNING)
                .notify(Config.getProject());
    }

    public static void error(String title, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("TestGit Notifications")
                .createNotification(title, content, NotificationType.ERROR)
                .notify(Config.getProject());
    }
}
