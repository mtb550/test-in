package org.testin.util.notifications;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Notifier {

    private static final Notifier INSTANCE = new Notifier();
    private final String GROUP_ID = "testin.notifications";

    @Getter
    private Notification pushToRemoteNotification;

    public static Notifier getInstance() {
        return INSTANCE;
    }

    public void softShow(@NotNull final String title, @NotNull final String message) {

        SwingUtilities.invokeLater(() -> {
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(Config.getProject());
            if (ideFrame == null || ideFrame.getStatusBar() == null) return;

            final JComponent statusBarComponent = ideFrame.getStatusBar().getComponent();

            final String htmlContent = String.format("<html><b>%s</b><br>%s</html>", title, message);

            final Balloon balloon = JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(htmlContent, MessageType.INFO, null)
                    .setFadeoutTime(4000)
                    .setAnimationCycle(200)
                    .createBalloon();

            final Point targetPoint = new Point(statusBarComponent.getWidth() - 30, statusBarComponent.getHeight() / 2);
            final RelativePoint relativePoint = new RelativePoint(statusBarComponent, targetPoint);

            balloon.show(relativePoint, Balloon.Position.above);
        });
    }

    public void info(final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(message, NotificationType.INFORMATION)
                .notify(Config.getProject());
    }

    public void warn(final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(message, NotificationType.WARNING)
                .notify(Config.getProject());
    }

    public void error(final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(message, NotificationType.ERROR)
                .notify(Config.getProject());
    }

    public void info(final @NotNull String title, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.INFORMATION)
                .notify(Config.getProject());
    }

    public void warn(final @NotNull String title, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.WARNING)
                .notify(Config.getProject());
    }

    public void error(final @NotNull String title, final @NotNull String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.ERROR)
                .notify(Config.getProject());
    }

    public void warnWithAction(final @NotNull String title, final @NotNull String message, final @NotNull String actionName, final @NotNull Runnable action) {
        final Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.WARNING);

        notification.addAction(NotificationAction.createSimple(actionName, action));
        notification.notify(Config.getProject());
    }

    public void infoWithAction(final @NotNull String title, final @NotNull String message, final @NotNull String actionName, final @NotNull Runnable action) {
        final Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.INFORMATION);

        notification.addAction(NotificationAction.createSimple(actionName, action));
        pushToRemoteNotification = notification;
        notification.notify(Config.getProject());
    }

    public void infoWithOpenAndCopy(final @NotNull String title, final @NotNull String message, final @NotNull File file) {
        final Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(title, message, NotificationType.INFORMATION);

        notification.addAction(NotificationAction.createSimple("Open report", () -> BrowserUtil.browse(file.toURI().toString())));
        NotificationAction copyAction = new NotificationAction("Copy path") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                CopyPasteManager.getInstance().setContents(new StringSelection(file.getAbsolutePath()));
            }
        };
        copyAction.getTemplatePresentation().setIcon(AllIcons.Actions.Copy);
        notification.addAction(copyAction);

        notification.notify(Config.getProject());
    }
}