package org.testin.indexer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Once;
import org.testin.logger.Logger;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where deleted test data goes: the desktop's own recycle bin, so a test set or
 * a case removed by mistake is recovered the way every other file on the
 * machine is - without this plugin having to be working for it.
 * <p>
 * Nothing of ours is left under the Testin root, which is the reason it is the
 * desktop's bin and not a folder of our own. A {@code .trash} inside the root
 * would be indexed, committed and synced unless the scanner, Git and the SFTP
 * transfer each learned to skip it, and deleted test data would travel to every
 * machine the tester works on.
 * <p>
 * The platform can put things into the trash and cannot take them out, so this
 * is a safety net rather than an undo. Taking a test case change back is
 * {@code TestCaseSnapshot}, which restores from its own snapshot and never
 * comes through here (#165).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Trash {

    /**
     * Said once per project - see {@link #sayThereIsNoBin}.
     */
    private static final @NotNull Key<Boolean> NO_BIN_SAID = Key.create("testin.trash.noBinSaid");

    /**
     * UC-INTERNAL-005, Rule-INTERNAL-036.
     * <p>
     * Moves a file, or a whole folder and everything under it, to the recycle
     * bin - and reports whether it went there.
     * <p>
     * False is an honest answer rather than a failure: a headless run and some
     * Linux sessions have no trash to move anything to, and a path that is
     * already gone has nothing to move. The caller deletes outright in either
     * case, which is what every delete did before this existed.
     */
    static boolean accepted(final @NotNull Project p, final @NotNull Path path) {
        if (!Files.exists(path)) return false;

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            Logger.debug("This desktop has no recycle bin; deleting " + path + " outright.");
            sayThereIsNoBin(p);
            return false;
        }

        try {
            if (Desktop.getDesktop().moveToTrash(path.toFile())) return true;

            Logger.warn("The recycle bin refused " + path + ", deleting it instead.");
            sayThereIsNoBin(p);
            return false;

        } catch (final Exception ex) {
            Logger.warn("Could not move " + path + " to the recycle bin, deleting it instead: " + ex.getMessage());
            sayThereIsNoBin(p);
            return false;
        }
    }

    /**
     * UC-INTERNAL-005, Rule-INTERNAL-036.
     * <p>
     * The removal this tester just made is not in any recycle bin.
     * <p>
     * Deleting outright is the right fallback - refusing to delete because the
     * bin is unavailable would be worse - and until now it was the right
     * fallback taken in silence. A tester who has been told their deletions are
     * recoverable looks in the bin, finds nothing, and has no way to know which
     * of the two happened (#66, finding 48).
     * <p>
     * Once per project, not once per file: removing a test set is one gesture
     * and a hundred deletions, and a hundred balloons saying the same sentence
     * is how a tester learns to dismiss all of them. A headless run and a Linux
     * session without a desktop bin answer this way for every delete they will
     * ever make, so the first is the one worth saying.
     */
    private static void sayThereIsNoBin(final @NotNull Project p) {
        if (!Once.claim(p, NO_BIN_SAID)) return;

        Services.getInstance(p, Notifier.class).info(p, Bundle.message("trash.none.title"), Bundle.message("trash.none.message"));
    }
}
