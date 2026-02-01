package testGit.util;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class Notifier {
    public static void notify(final Project project, final String groupId, final String title, final String content, final NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(groupId)
                .createNotification(title, content, type)
                .notify(project);
    }
}
