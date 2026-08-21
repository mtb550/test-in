package org.testin.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

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
 * None of them can be canceled. An import writes one file per test case and an
 * export or a report writes one file whole, so a stop part-way through leaves
 * something half-made that nobody asked for; the bar moves and nothing
 * interrupts it.
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
    public static void run(final @NotNull Project p, final @NotNull String title,
                           final @NotNull String whatFailed,
                           final @NotNull Consumer<@NotNull ProgressIndicator> work) {

        ProgressManager.getInstance().run(new Task.Backgroundable(p, title, false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    work.accept(indicator);
                } catch (final Exception ex) {
                    Logger.error(whatFailed + ": " + ex.getMessage());
                    Services.getInstance(p, Notifier.class).error(p, whatFailed, String.valueOf(ex.getMessage()));
                }
            }
        });
    }
}
