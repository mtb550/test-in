package org.testin.indexer;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.io.FileUtil;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A copy of a node kept aside while CTRL+Z can still reach it.
 * <p>
 * The recycle bin holds the tester's copy and is the one they can find without
 * this plugin - but the platform can put things into it and cannot take them
 * out, so a removal that is meant to be undoable needs a second copy somewhere
 * we control. This is that copy, and it lives as long as the operation that can
 * restore it: pushed off the end of that surface's history and it goes, and
 * whatever a run ends still holding is cleared by the next one.
 * <p>
 * Under the IDE's own system directory, beside the sync baselines, rather than
 * anywhere below the Testin root. A folder under the root would be indexed,
 * committed and synced unless three other places learned to skip it, and
 * deleted test data would travel to every machine the tester works on.
 */
@Service(Service.Level.APP)
@NoArgsConstructor
public final class DeletedNodes {

    private final @NotNull Path staging = Path.of(PathManager.getSystemPath(), "testin", "deleted");

    private final @NotNull AtomicBoolean swept = new AtomicBoolean();

    /**
     * Clears whatever the last run left behind, once per IDE run.
     * <p>
     * At startup rather than at shutdown, because the copies that matter are
     * exactly the ones a shutdown never reached - an IDE that crashed or was
     * killed. Nothing here can be reached by a session that has only just
     * started: an undo stack begins empty, so every copy left over belongs to a
     * run that has ended.
     * <p>
     * Once per run and not once per project: two projects open share this
     * directory, and the second one to open must not clear what the first is
     * still holding.
     */
    public void sweep() {
        if (!swept.compareAndSet(false, true)) return;
        if (!Files.isDirectory(staging)) return;

        if (FileUtil.delete(staging.toFile())) Logger.info("Cleared the copies kept for undo by the previous run.");
        else Logger.warn("Could not clear " + staging + "; copies from the previous run are still there.");
    }

    /**
     * Copies a node aside and answers where it went, or nothing at all when the
     * copy failed - in which case the removal still happens and simply cannot be
     * undone, which is what every removal did before this existed.
     * <p>
     * Its own folder per removal, named by a fresh id, so two sets removed under
     * the same name are two things to put back rather than one overwriting the
     * other.
     */
    public @NotNull Optional<Path> keep(final @NotNull Path node) {
        if (!Files.exists(node)) return Optional.empty();

        final @NotNull Path kept = staging.resolve(UUID.randomUUID().toString()).resolve(node.getFileName().toString());

        try {
            Files.createDirectories(kept.getParent());

            if (Files.isDirectory(node)) FileUtil.copyDir(node.toFile(), kept.toFile());
            else Files.copy(node, kept, StandardCopyOption.REPLACE_EXISTING);

            return Optional.of(kept);

        } catch (final Exception ex) {
            Logger.warn("Could not keep " + node + " aside, so removing it will not be undoable: " + ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Puts a kept node back where it was removed from, and says whether it went.
     * <p>
     * Refuses a path something already occupies. A node created under the same
     * name since the removal is a tester's work, and an undo that wrote over it
     * would be destroying something to restore something.
     */
    public boolean putBack(final @NotNull Path kept, final @NotNull Path original) {
        if (Files.exists(original)) {
            Logger.warn("Not restoring " + original + ": something is there already.");
            return false;
        }

        try {
            Services.getInstance(OwnWrites.class).record(original);
            Files.createDirectories(original.getParent());

            if (Files.isDirectory(kept)) FileUtil.copyDir(kept.toFile(), original.toFile());
            else Files.copy(kept, original, StandardCopyOption.REPLACE_EXISTING);

            return true;

        } catch (final Exception ex) {
            Logger.error("Could not restore " + original + " from " + kept + ": " + ex.getMessage());
            return false;
        }
    }

    /**
     * Nobody can reach the operation holding this any more, so the copy goes for
     * good. Outright rather than to the recycle bin: the bin already took the
     * tester's own copy when the node was removed, and a second one arriving
     * later would be a duplicate they never asked for.
     */
    public void forget(final @NotNull Path kept) {
        if (!FileUtil.delete(kept.getParent().toFile()))
            Logger.warn("Left a kept copy of a removed node behind at " + kept.getParent());
    }
}
