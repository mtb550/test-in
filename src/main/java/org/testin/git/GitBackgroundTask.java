package org.testin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * The one background-task shape used by all git workflows: run the work on a
 * background thread with an indeterminate indicator; on failure, hand the
 * exception to the error handler on the EDT. Success continuations schedule
 * their own EDT work inside the body.
 */
public final class GitBackgroundTask extends Task.Backgroundable {

    private final @NotNull GitTaskWork work;
    private final @NotNull Consumer<Exception> onError;

    public GitBackgroundTask(final @NotNull Project p, final @NotNull String title, final boolean cancellable,
                             final @NotNull GitTaskWork work, final @NotNull Consumer<Exception> onError) {
        super(p, title, cancellable);
        this.work = work;
        this.onError = onError;
    }

    public static void run(final @NotNull Project p, final @NotNull String title, final boolean cancellable,
                           final @NotNull GitTaskWork work, final @NotNull Consumer<Exception> onError) {
        ProgressManager.getInstance().run(new GitBackgroundTask(p, title, cancellable, work, onError));
    }

    @Override
    public void run(final @NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        try {
            work.run(indicator);
        } catch (final Exception ex) {
            ApplicationManager.getApplication().invokeLater(() -> onError.accept(ex));
        }
    }
}
