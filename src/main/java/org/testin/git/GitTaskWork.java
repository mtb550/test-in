package org.testin.git;

import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

/**
 * Background body of a {@link GitBackgroundTask}; may throw, the task reports
 * the failure on the EDT.
 * <p>
 * The {@code throws} stays deliberately, and is one of the two exceptions to the
 * rule in CLAUDE.md that a method handles its own failures. This is a functional
 * interface whose whole point is to let the lambda report failure to the task's
 * error handler; removing it would force every git lambda to grow a try/catch
 * and to find its own way back to that handler (#63).
 */
@FunctionalInterface
public interface GitTaskWork {

    // RedundantThrows is right about the fact and wrong about the conclusion: no
    // implementation throws a checked exception, because #63 converted the git
    // service to return its failures. The declaration is what lets a lambda
    // raise one when it needs the task's error handler, which
    // ViewPendingCommitsAction.finishRebase does.
    @SuppressWarnings("RedundantThrows")
    void run(final @NotNull ProgressIndicator indicator) throws Exception;
}
