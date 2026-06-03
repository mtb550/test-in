package org.testin.util.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Notifier {

    private static final Notifier INSTANCE = new Notifier();
    private final String GROUP_ID = "testin.notifications";

    public static Notifier getInstance() {
        return INSTANCE;
    }

    public void softShow(@NotNull final Project project, @NotNull final String title, @NotNull final String message) {

        SwingUtilities.invokeLater(() -> {
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
            if (ideFrame == null || ideFrame.getStatusBar() == null) return;

            final JComponent statusBarComponent = ideFrame.getStatusBar().getComponent();
            if (statusBarComponent == null) return;

            final String htmlContent = String.format("<html><b>%s</b><br>%s</html>", title, message);

            final Balloon balloon = JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(htmlContent, MessageType.INFO, null)
                    .setFadeoutTime(5000)
                    .setAnimationCycle(200)
                    .createBalloon();

            final Point targetPoint = new Point(statusBarComponent.getWidth() - 30, statusBarComponent.getHeight() / 2);
            final RelativePoint relativePoint = new RelativePoint(statusBarComponent, targetPoint);

            balloon.show(relativePoint, Balloon.Position.above);
        });
    }

    public void softShow(@NotNull final Project project, @NotNull final String message) {

        SwingUtilities.invokeLater(() -> {
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
            if (ideFrame == null || ideFrame.getStatusBar() == null) return;

            final JComponent statusBarComponent = ideFrame.getStatusBar().getComponent();
            if (statusBarComponent == null) return;

            final String htmlContent = String.format("<html>%s</html>", message);

            final Balloon balloon = JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(htmlContent, MessageType.INFO, null)
                    .setFadeoutTime(5000)
                    .setAnimationCycle(200)
                    .createBalloon();

            final Point targetPoint = new Point(statusBarComponent.getWidth() - 30, statusBarComponent.getHeight() / 2);
            final RelativePoint relativePoint = new RelativePoint(statusBarComponent, targetPoint);

            balloon.show(relativePoint, Balloon.Position.above);
        });
    }

    public void info(final @NotNull Project project, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(message, NotificationType.INFORMATION)
                .notify(project);
    }

    public void warn(final @NotNull Project project, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(message, NotificationType.WARNING)
                .notify(project);
    }

    public void error(final @NotNull Project project, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(message, NotificationType.ERROR)
                .notify(project);
    }

    public void info(final @NotNull Project project, final @NotNull String title, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.INFORMATION)
                .notify(project);
    }

    public void warn(final @NotNull Project project, final @NotNull String title, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.WARNING)
                .notify(project);
    }

    public void error(final @NotNull Project project, final @NotNull String title, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.ERROR)
                .notify(project);
    }

    public void warnWithAction(final @NotNull Project project, final @NotNull String title, final @NotNull String message, final @NotNull String actionName, final @NotNull Runnable action) {
        final Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.WARNING);

        notification.addAction(NotificationAction.createSimple(actionName, action));
        notification.notify(project);
    }

    public Notification infoWithActions(final @NotNull Project project, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        final Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.INFORMATION);

        for (NotificationAction action : actions) {
            notification.addAction(action);
        }

        notification.notify(project);
        return notification;
    }
}