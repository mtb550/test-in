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

    private static final @NotNull String GROUP_ID = "testin.notifications";

    public void softShow(final @NotNull Project p, final @NotNull String title, final @NotNull String message) {
        showBalloon(p, String.format("<html><b>%s</b><br>%s</html>", title, message));
    }

    public void softShow(final @NotNull Project p, final @NotNull String message) {
        showBalloon(p, String.format("<html>%s</html>", message));
    }

    /**
     * The tester typed a name that is already taken.
     * <p>
     * One sentence for one situation, whichever action they reached it through -
     * creating a test project, creating any node in the tree, or renaming one.
     * It fades rather than going in the log: it is feedback on what they just
     * typed, not a failure worth keeping beside real ones (#62).
     */
    public void softShowExists(final @NotNull Project p, final @NotNull String name) {
        softShow(p, name + " Already Exists");
    }

    /**
     * Confirms an operation that ran over a selection: "Node copied" for one,
     * "Nodes copied 3" for several. Here rather than at the call sites so that
     * every bulk action pluralizes and counts the same way (#62).
     */
    public void softShowCounted(final @NotNull Project p, final @NotNull String noun,
                                final @NotNull String outcome, final int count) {
        softShow(p, count == 1 ? noun + " " + outcome : noun + "s " + outcome + " " + count);
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

    /**
     * A link on a notification, and the notification goes away when it is
     * clicked.
     * <p>
     * Every one of these is a one-shot offer - initialize the repository,
     * continue the rebase, open the settings - so the notification that made the
     * offer has nothing left to say once it is taken. Built here rather than at
     * the call sites because {@code NotificationAction.createSimple} does not
     * expire and nothing fails when it does not: the notification simply stays,
     * with its link still live. On the conflict notification that meant Abort
     * could be clicked, and then Continue, on a rebase that no longer existed.
     */
    public @NotNull NotificationAction action(final @NotNull String name, final @NotNull Runnable action) {
        return NotificationAction.createSimpleExpiring(name, action);
    }

    public void warnWithAction(final @NotNull Project p, final @NotNull String title, final @NotNull String message, final @NotNull String actionName, final @NotNull Runnable action) {
        warnWithActions(p, title, message, action(actionName, action));
    }

    public void warnWithActions(final @NotNull Project p, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        notify(p, title, message, NotificationType.WARNING, actions);
    }

    public void infoWithActions(final @NotNull Project p, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        notify(p, title, message, NotificationType.INFORMATION, actions);
    }

    /**
     * A failure the tester can do something about, with the something attached.
     * <p>
     * An error with no way forward is just news; this is for the ones where the
     * answer is "try that again" - a push that could not reach the remote, say.
     */
    public void errorWithActions(final @NotNull Project p, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        notify(p, title, message, NotificationType.ERROR, actions);
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
