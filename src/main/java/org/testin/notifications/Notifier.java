/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.testin.logger.Logger;
import org.testin.util.Bundle;

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
     * A refusal about one thing, in the words {@link Refused} keeps.
     * <p>
     * Composing the sentence is not this class's job - delivering it is. Five
     * refusals used to be a method each here, which made the class that hands
     * messages to the platform also the class that decides what they say.
     */
    public void softRefuse(final @NotNull Project p, final @NotNull Refused refusal, final @NotNull String name) {
        softRefuse(p, refusal.about(name));
    }

    /**
     * Rule-TREE-PANEL-007, Rule-EDITOR-PANEL-008.
     * <p>
     * Confirms an operation that ran over a selection: <i>Copied</i> for one,
     * <i>Copied 3</i> for several. Here rather than at the call sites so that
     * every bulk action counts the same way (#62).
     * <p>
     * What the sentence looks like is {@link Done#counted}'s, not this class's -
     * delivering it is the job here.
     */
    public void softShowCounted(final @NotNull Project p, final @NotNull String outcome, final int count) {
        // Nothing happened, so there is nothing to confirm. It used to say
        // "Removed 0", which is a balloon telling the tester that their gesture
        // reached nothing - news only if something was expected, and the caller
        // that expected something says so itself (#269).
        if (count <= 0) return;

        softShow(p, Done.counted(outcome, count));
    }

    /**
     * Rule-TREE-PANEL-007, Rule-EDITOR-PANEL-008.
     * <p>
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
            // has no status bar to anchor to. Nothing is raised in its place -
            // a second notification about a failed notification is noise - but
            // it is written down, because what these balloons carry is every
            // "exported", "imported" and "synced" the plugin says, and an export
            // that finished with nobody told is not the same as one that did not
            // finish (#270).
            Optional.ofNullable(WindowManager.getInstance().getIdeFrame(p))
                    .map(IdeFrame::getStatusBar)
                    .map(StatusBar::getComponent)
                    .ifPresentOrElse(statusBarComponent -> {
                        final @NotNull Balloon balloon = JBPopupFactory.getInstance()
                                .createHtmlTextBalloonBuilder(htmlContent, type, null)
                                .setFadeoutTime(5000)
                                .setAnimationCycle(200)
                                .createBalloon();

                        final @NotNull Point targetPoint = new Point(statusBarComponent.getWidth() - 30,
                                statusBarComponent.getHeight() / 2);
                        balloon.show(new RelativePoint(statusBarComponent, targetPoint), Balloon.Position.above);
                    },
                    () -> Logger.info("Nowhere to show this, so it is only here: " + htmlContent));
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
        final @NotNull NotificationAction copy = action(Bundle.message("notification.copy.path"),
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
