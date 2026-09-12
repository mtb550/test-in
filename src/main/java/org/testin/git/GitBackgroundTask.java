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

package org.testin.git;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * The one background-task shape used by all git workflows: run the work on a
 * background thread with an indeterminate indicator, and hand a failure to the
 * error handler on that same thread. Both continuations schedule their own EDT
 * work, which is the rule this class exists to keep.
 * <p>
 * The error handler used to be dispatched to the EDT, and that was wrong for the
 * one thing a git error handler most often needs to do: ask git another question.
 * Deciding whether a failed push stopped on a conflict means running
 * {@code git status}, and running a git command on the EDT trips the platform's
 * own assertion - "Should not wait for built-in server on EDT" - from inside
 * {@code git4idea}'s HTTP authentication setup. A stack trace in the log instead of the
 * abort-or-continue offer the tester needed.
 */
public final class GitBackgroundTask extends Task.Backgroundable {

    private final @NotNull GitTaskWork work;
    private final @NotNull Consumer<Exception> onError;

    public GitBackgroundTask(final @NotNull Project p, final @NotNull String title, final boolean cancellable, final @NotNull GitTaskWork work, final @NotNull Consumer<Exception> onError) {
        super(p, title, cancellable);
        this.work = work;
        this.onError = onError;
    }

    // Rule-SHARE-005
    public static void run(final @NotNull Project p, final @NotNull String title, final boolean cancellable, final @NotNull GitTaskWork work, final @NotNull Consumer<Exception> onError) {
        ProgressManager.getInstance().run(new GitBackgroundTask(p, title, cancellable, work, onError));
    }

    @Override
    public void run(final @NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        try {
            work.run(indicator);
        } catch (final ProcessCanceledException stopped) {
            // A cancel is not an error, so it does not go to the error handler.
            // The platform is the one that knows what to do with it.
            throw stopped;
        } catch (final Exception ex) {
            onError.accept(ex);
        }
    }
}
