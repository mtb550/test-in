package org.testin.services;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;

import java.util.function.Consumer;

/**
 * Work that runs off the EDT under the IDE's own progress bar.
 * <p>
 * A tester starts four long operations from a dialog: importing a sheet,
 * exporting one, generating a report, creating a test run. Each used to run with
 * the dialog still on screen, and where the work was on the EDT, frozen with it.
 * <p>
 * The dialog closes on the button now, and the work reports itself here (#87).
 * <p>
 * All of them can be canceled, unlike the Git tasks. None of the four can leave
 * the repository in a state the tester cannot see, which is the reason a push or
 * a rebase cannot be stopped and these can (#257). The reasoning per operation
 * is at the one place that builds the task, in {@link #run}.
 * <p>
 * A failure is logged and shown once. It is caught here because a task that
 * throws past {@code run} leaves the bar up and says nothing.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BackgroundWork {

    /**
     * Runs {@code work} under a progress bar titled for the operation.
     *
     * @param title      what the bar says, naming the operation and what it is
     *                   working on - "Importing into App Activation"
     * @param whatFailed the heading of the message if it throws - "Import Failed"
     * @param work       the work itself. It is handed the indicator, so anything
     *                   that knows how much it has to do can say so with
     *                   {@link ProgressIndicator#setFraction}. Anything that
     *                   cannot leaves the bar as it found it.
     */
    public static void run(final @NotNull Project p, final @NotNull String title, final @NotNull String whatFailed, final @NotNull Consumer<@NotNull ProgressIndicator> work) {

        // Cancellable, unlike the Git tasks. A report writes one file at the end
        // and an export writes one file at the end, so stopping leaves nothing
        // behind at all; an import writes a test case at a time and already says
        // how many were written when it stops part way (Rule-SHARE-037). None of
        // the three can leave the repository in a state the tester cannot see,
        // which is the reason a push or a rebase cannot be stopped (#257).
        ProgressManager.getInstance().run(new Task.Backgroundable(p, title, true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    work.accept(indicator);
                } catch (final ProcessCanceledException stopped) {
                    // The tester pressed Cancel. That is an answer, not a
                    // failure, and they already know they gave it - so it is
                    // handed back to the platform, which closes the bar the way
                    // it closes every canceled task. Caught before the line
                    // below because that one would have shown them "Import
                    // Failed" for doing what the Cancel button is for
                    // (#66, finding 83).
                    throw stopped;
                } catch (final Exception ex) {
                    Logger.error(whatFailed + ": " + ex.getMessage());
                    Services.getInstance(p, Notifier.class).error(p, whatFailed, String.valueOf(ex.getMessage()));
                }
            }
        });
    }
}
