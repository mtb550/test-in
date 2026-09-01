package org.testin.indexer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
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
     * Moves a file, or a whole folder and everything under it, to the recycle
     * bin - and reports whether it went there.
     * <p>
     * False is an honest answer rather than a failure: a headless run and some
     * Linux sessions have no trash to move anything to, and a path that is
     * already gone has nothing to move. The caller deletes outright in either
     * case, which is what every delete did before this existed.
     */
    static boolean accepted(final @NotNull Path path) {
        if (!Files.exists(path)) return false;
        if (!Desktop.isDesktopSupported()) return false;

        final @NotNull Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            Logger.debug("This desktop has no recycle bin; deleting " + path + " outright.");
            return false;
        }

        try {
            return desktop.moveToTrash(path.toFile());
        } catch (final Exception ex) {
            Logger.warn("Could not move " + path + " to the recycle bin, deleting it instead: " + ex.getMessage());
            return false;
        }
    }
}
