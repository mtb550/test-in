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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Work that runs off the EDT under the IDE's own progress bar.
 * <p>
 * A tester starts four long operations from a dialog: importing a sheet,
 * exporting one, generating a report, creating a test run. Each used to run with
 * the dialog still on screen, and where the work was on the EDT, frozen with it.
 * <p>
 * The dialog closes on the button now, and the work reports itself here (#87).
 * <p>
 * Those four can be canceled, unlike the Git tasks. None of them can leave the
 * repository in a state the tester cannot see, which is the reason a push or a
 * rebase cannot be stopped and these can (#257). Sending a bug report cannot be
 * stopped either, for the same kind of reason: a cancel after GitHub created the
 * issue would leave an issue nothing links to (#28).
 * <p>
 * A failure is logged and shown once. It is caught here because a task that
 * throws past {@code run} leaves the bar up and says nothing.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BackgroundWork {

    private static final @NotNull Runnable NOTHING = () -> {
    };

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
        run(p, title, whatFailed, true, work, NOTHING, NOTHING);
    }

    /**
     * The same, for work that answers something, saying whether it can be
     * canceled and what happens after it.
     * <p>
     * The answer is handed to {@code onSuccess} here, so no caller keeps a holder
     * of its own for it to cross from the task's thread to the EDT. The two hooks
     * are what lets work that holds something - a link disabled while its work
     * runs - let go of it on every way out, a cancel and a failure included (#28).
     *
     * @param cancellable false for work that must not stop half way
     * @param onSuccess   on the EDT, with the answer, once the work finished
     *                    without failing and without being canceled
     * @param onFinished  on the EDT, always, and after {@code onSuccess}
     */
    public static <T> void run(final @NotNull Project p, final @NotNull String title, final @NotNull String whatFailed, final boolean cancellable, final @NotNull Function<@NotNull ProgressIndicator, @NotNull T> work, final @NotNull Consumer<@NotNull T> onSuccess, final @NotNull Runnable onFinished) {
        final @NotNull AtomicReference<Optional<T>> answer = new AtomicReference<>(Optional.empty());
        run(p, title, whatFailed, cancellable, indicator -> answer.set(Optional.of(work.apply(indicator))), () -> answer.get().ifPresent(onSuccess), onFinished);
    }

    private static void run(final @NotNull Project p, final @NotNull String title, final @NotNull String whatFailed, final boolean cancellable, final @NotNull Consumer<@NotNull ProgressIndicator> work, final @NotNull Runnable onSuccess, final @NotNull Runnable onFinished) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, title, cancellable) {

            /**
             * Set on the task's thread and read on the EDT. The platform calls
             * {@code onSuccess} for a run that returned, and a failure caught
             * below returns too.
             */
            private volatile boolean failed;

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
                    failed = true;

                    // The exception's own name when it carries no message: a
                    // NullPointerException has none, and the tester was shown the
                    // word "null" (#312, A96).
                    final @NotNull String reason = Objects.requireNonNullElse(ex.getMessage(), ex.toString());
                    Logger.error(whatFailed + ": " + reason);
                    Services.getInstance(p, Notifier.class).error(p, whatFailed, reason);
                }
            }

            @Override
            public void onSuccess() {
                if (!failed) onSuccess.run();
            }

            @Override
            public void onFinished() {
                onFinished.run();
            }
        });
    }
}
