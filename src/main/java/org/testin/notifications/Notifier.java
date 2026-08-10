package org.testin.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
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
@Service(Service.Level.PROJECT)
public final class Notifier {

    private final String GROUP_ID = "testin.notifications";

    public void softShow(final @NotNull Project p, final @NotNull String title, final @NotNull String message) {
        showBalloon(p, String.format("<html><b>%s</b><br>%s</html>", title, message));
    }

    public void softShow(final @NotNull Project p, final @NotNull String message) {
        showBalloon(p, String.format("<html>%s</html>", message));
    }

    /**
     * Lightweight fading balloon anchored to the IDE status bar.
     */
    private void showBalloon(final @NotNull Project p, final @NotNull String htmlContent) {
        ApplicationManager.getApplication().invokeLater(() -> {
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(p);
            if (ideFrame == null || ideFrame.getStatusBar() == null) return;

            final JComponent statusBarComponent = ideFrame.getStatusBar().getComponent();
            if (statusBarComponent == null) return;

            final Balloon balloon = JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(htmlContent, MessageType.INFO, null)
                    .setFadeoutTime(5000)
                    .setAnimationCycle(200)
                    .createBalloon();

            final Point targetPoint = new Point(statusBarComponent.getWidth() - 30, statusBarComponent.getHeight() / 2);
            balloon.show(new RelativePoint(statusBarComponent, targetPoint), Balloon.Position.above);
        });
    }

    public void info(final @NotNull Project p, final @NotNull String message) {
        notify(p, null, message, NotificationType.INFORMATION);
    }

    public void warn(final @NotNull Project p, final @NotNull String message) {
        notify(p, null, message, NotificationType.WARNING);
    }

    public void error(final @NotNull Project p, final @NotNull String message) {
        notify(p, null, message, NotificationType.ERROR);
    }

    public void info(final @NotNull Project p, final @NotNull String title, final @NotNull String message) {
        notify(p, title, message, NotificationType.INFORMATION);
    }

    public void warn(final @NotNull Project p, final @NotNull String title, final @NotNull String message) {
        notify(p, title, message, NotificationType.WARNING);
    }

    public void error(final @NotNull Project p, final @NotNull String title, final @NotNull String message) {
        notify(p, title, message, NotificationType.ERROR);
    }

    public void warnWithAction(final @NotNull Project p, final @NotNull String title, final @NotNull String message, final @NotNull String actionName, final @NotNull Runnable action) {
        warnWithActions(p, title, message, NotificationAction.createSimple(actionName, action));
    }

    public void warnWithActions(final @NotNull Project p, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        notify(p, title, message, NotificationType.WARNING, actions);
    }

    public Notification infoWithActions(final @NotNull Project p, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        return notify(p, title, message, NotificationType.INFORMATION, actions);
    }

    private Notification notify(final @NotNull Project p, final String title, final @NotNull String message,
                                final @NotNull NotificationType type, final @NotNull NotificationAction... actions) {
        final Notification notification = title == null
                ? NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID).createNotification(message, type)
                : NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID).createNotification(title, message, type);

        for (final NotificationAction action : actions) notification.addAction(action);
        notification.notify(p);
        return notification;
    }
}
