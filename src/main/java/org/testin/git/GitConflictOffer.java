package org.testin.git;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.List;

/**
 * The three ways out of a rebase that stopped on a conflict.
 * <p>
 * One balloon, because there were two with the same title, the same message and
 * the same three captions - and the middle button did different things
 * depending on which of them the tester was looking at. From the Sync button
 * Continue rebase refreshed and re-indexed without pushing; from Push it pushed
 * without refreshing; and the Resolve button on the sync route already did
 * both, which is the ending both routes now take.
 * <p>
 * What each button does still belongs to the route that raised the offer - the
 * push route has a remote and a branch in hand and the sync route does not.
 * What is shared is the offer itself: the title, the sentence naming the
 * conflicting files, and the fact that there are exactly three ways out.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GitConflictOffer {

    // UC-SHARE-017
    static void show(final @NotNull Project p, final @NotNull List<String> conflicting, final @NotNull Runnable onResolve, final @NotNull Runnable onContinue, final @NotNull Runnable onAbort) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        notifier.warnWithActions(
                p,
                Bundle.message("git.conflicts.title"),
                GitRefs.conflictMessage(conflicting),
                notifier.action(Bundle.message("git.conflicts.resolve"), onResolve),
                notifier.action(Bundle.message("git.conflicts.continue.rebase"), onContinue),
                notifier.action(Bundle.message("git.conflicts.abort.rebase"), onAbort));
    }
}
