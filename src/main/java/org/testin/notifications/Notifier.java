package org.testin.notifications;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import java.awt.datatransfer.StringSelection;
import java.io.File;
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
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class Notifier {

    private static final @NotNull String GROUP_ID = "testin.notifications";

    public void softShow(final @NotNull Project p, final @NotNull String title, final @NotNull String message) {
        showBalloon(p, String.format("<html><b>%s</b><br>%s</html>", title, message), MessageType.INFO);
    }

    public void softShow(final @NotNull Project p, final @NotNull String message) {
        showBalloon(p, String.format("<html>%s</html>", message), MessageType.INFO);
    }

    /**
     * The same balloon, in red, for an action that did not happen.
     * <p>
     * A confirmation and a refusal read identically at a glance when both carry
     * the blue information icon, and a refusal is the one the tester has to act
     * on - it is telling them to do something differently. It still fades: this
     * is feedback on the gesture they just made, not a failure worth keeping
     * beside real ones (#62).
     */
    public void softRefuse(final @NotNull Project p, final @NotNull String message) {
        showBalloon(p, String.format("<html>%s</html>", message), MessageType.ERROR);
    }

    public void softRefuse(final @NotNull Project p, final @NotNull String title, final @NotNull String message) {
        showBalloon(p, String.format("<html><b>%s</b><br>%s</html>", title, message), MessageType.ERROR);
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
        softRefuse(p, name + " Already Exists");
    }

    /**
     * The tester acted on a test case that has no generated method.
     * <p>
     * One sentence for one situation, whichever action they reached it through -
     * running the case, or editing it and expecting the code to follow. Both used
     * to give up in silence, each in its own way: the runner ran whatever else
     * the class held, and an edit changed the case and left the code alone (#34,
     * #66 finding 19).
     * <p>
     * It fades rather than going in the log: it is feedback on what they just
     * did, and the remedy - generate the code - is a keystroke away.
     */
    public void softShowNoGeneratedCode(final @NotNull Project p, final @NotNull String testCase) {
        softRefuse(p, testCase + " has no generated code yet");
    }

    /**
     * The tester asked to run a node that has nothing left to run.
     * <p>
     * One sentence for two situations that read the same to them: a test set
     * holding no cases at all, and a run whose cases have all been judged
     * already. Both are the tree saying "there is nothing here for me to start",
     * and neither is a failure worth keeping beside real ones.
     */
    public void softRefuseNothingToRun(final @NotNull Project p, final @NotNull String name) {
        softRefuse(p, name + " has no test cases to run");
    }

    /**
     * The tester pressed start on a walk with nowhere to land.
     * <p>
     * Its own sentence rather than the one above: that one is for a test run
     * with nothing left in it, and this one is usually a filter with everything
     * still behind it. Telling a tester their test run has no test cases when a
     * filter is what emptied the screen sends them looking for cases that are
     * still there (#215).
     * <p>
     * It fades, like every other answer to a gesture the tester just made.
     */
    public void softRefuseNothingShowing(final @NotNull Project p, final @NotNull String name) {
        softRefuse(p, "Nothing is showing in " + name + " to execute");
    }

    /**
     * The tester asked to run something that is already running.
     * <p>
     * Its own sentence rather than the one below: a run with cases still going
     * has plenty left to run, and telling them it has nothing would send them
     * looking for cases that are on screen in front of them.
     */
    public void softRefuseAlreadyRunning(final @NotNull Project p, final @NotNull String name) {
        softRefuse(p, name + " is already running");
    }

    /**
     * Confirms an operation that ran over a selection: "Node copied" for one,
     * "Nodes copied 3" for several. Here rather than at the call sites so that
     * every bulk action pluralizes and counts the same way (#62).
     */
    public void softShowCounted(final @NotNull Project p, final @NotNull String outcome, final int count) {
        softShow(p, count == 1 ? outcome : outcome + " " + count);
    }

    /**
     * The same two, taking the outcome rather than a word for it.
     * <p>
     * Preferred over the String forms wherever the outcome is one of the
     * {@link Done} constants, which is almost everywhere: the enum is what keeps
     * the past-tense rule from being a thing each call site remembers on its
     * own.
     */
    public void softShow(final @NotNull Project p, final @NotNull Done done) {
        softShow(p, done.getOutcome());
    }

    public void softShowCounted(final @NotNull Project p, final @NotNull Done done, final int count) {
        softShowCounted(p, done.getOutcome(), count);
    }

    /**
     * Lightweight fading balloon anchored to the IDE status bar.
     */
    private void showBalloon(final @NotNull Project p, final @NotNull String htmlContent, final @NotNull MessageType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // A project window that is closing, or has not opened its frame yet,
            // has no status bar to anchor to - and a balloon nobody can see is
            // not a failure worth reporting.
            Optional.ofNullable(WindowManager.getInstance().getIdeFrame(p))
                    .map(IdeFrame::getStatusBar)
                    .map(StatusBar::getComponent)
                    .ifPresent(statusBarComponent -> {
                        final @NotNull Balloon balloon = JBPopupFactory.getInstance()
                                .createHtmlTextBalloonBuilder(htmlContent, type, null)
                                .setFadeoutTime(5000)
                                .setAnimationCycle(200)
                                .createBalloon();

                        final @NotNull Point targetPoint = new Point(statusBarComponent.getWidth() - 30,
                                statusBarComponent.getHeight() / 2);
                        balloon.show(new RelativePoint(statusBarComponent, targetPoint), Balloon.Position.above);
                    });
        });
    }

    /**
     * An error with nothing written above the message: the message is the whole
     * of it.
     */
    private static final @NotNull String NO_TITLE = "";

    public void error(final @NotNull Project p, final @NotNull String message) {
        notify(p, NO_TITLE, message, NotificationType.ERROR);
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

    /**
     * Puts a written file's full path on the clipboard.
     * <p>
     * Here beside the other notification actions rather than hand-built at the
     * one notification that offered it, so every notification about a file the
     * plugin wrote can offer the same thing.
     */
    public @NotNull NotificationAction copyPath(final @NotNull File file) {
        final @NotNull NotificationAction copy = action("Copy path",
                () -> CopyPasteManager.getInstance().setContents(new StringSelection(file.getAbsolutePath())));

        copy.getTemplatePresentation().setIcon(AllIcons.Actions.Copy);

        return copy;
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

    private void notify(final @NotNull Project p, final @NotNull String title, final @NotNull String message, final @NotNull NotificationType type, final @NotNull NotificationAction... actions) {
        // The platform has one overload with a title and one without, and picks
        // by which is called - so "no title" needs a value to be chosen by. It
        // used to be a null, which the annotation sweep then declared impossible
        // while the one caller that passes it went on passing it (#93).
        final @NotNull Notification notification = title.isEmpty()
                ? NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID).createNotification(message, type)
                : NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID).createNotification(title, message, type);

        for (final NotificationAction action : actions) notification.addAction(action);
        notification.notify(p);
    }
}
