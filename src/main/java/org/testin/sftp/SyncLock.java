package org.testin.sftp;

import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * One sync at a time against a project on the server (#94).
 * <p>
 * Two testers pressing Sync in the same minute both read the manifest, both
 * decide from it, and both write it back - so the second one's manifest
 * describes a server that also holds the first one's files, and the sync after
 * that reads the difference as somebody's deletions. Nothing about the transfer
 * itself is unsafe; the record of it is.
 * <p>
 * A directory is the lock, because making one is the single thing SFTP does
 * atomically: two clients calling {@code mkdir} on the same path cannot both be
 * told they made it. Inside it goes a line saying who holds it and since when,
 * so the tester who is refused is told something they can act on rather than
 * "busy".
 */
public record SyncLock(@NotNull SftpTransport transport) {

    /**
     * At the top of the project rather than under {@code .testin}, because the
     * atomicity is in creating the directory itself - a lock that needs its
     * parent created first is a lock with a race in front of it.
     */
    private static final @NotNull String FOLDER = ".testin-lock";

    private static final @NotNull String HOLDER = FOLDER + "/holder.txt";

    /**
     * Takes the lock, and says who has it when somebody else does.
     * <p>
     * Empty means taken by this caller and nobody else. Present means refused,
     * and carries the sentence the tester reads.
     */
    public @NotNull Optional<String> takenBy(final @NotNull String tester) {
        if (!transport.makeDirectory(FOLDER)) return Optional.of(whoHasIt());

        // After the directory, so the claim is what the other client sees first
        // and this is only ever the explanation of a claim already made.
        transport.write(HOLDER, (tester + "\n" + ZonedDateTime.now()).getBytes(StandardCharsets.UTF_8));
        return Optional.empty();
    }

    /**
     * Whoever holds it, in the tester's words.
     * <p>
     * A lock with no readable holder is still a lock. It happens when the holder
     * has made the directory and not yet written the line, which is a window of
     * milliseconds, and after a client died between the two - so this says what
     * it knows rather than treating an unreadable file as no lock at all.
     */
    private @NotNull String whoHasIt() {
        try {
            final @NotNull String[] said = new String(transport.read(HOLDER), StandardCharsets.UTF_8).split("\n");
            if (said.length < 2) return "Another sync is running.";

            return said[0] + " started a sync at " + said[1] + ".";
        } catch (final RuntimeException ex) {
            Logger.warn("The sync lock is held but its holder could not be read: " + ex.getMessage());
            return "Another sync is running.";
        }
    }

    /**
     * Gives it back.
     * <p>
     * In a finally, always: a lock left behind by a failed sync blocks every
     * tester on the team until somebody deletes a hidden folder over SSH, which
     * is not a thing to ask of them.
     */
    public void release() {
        try {
            transport.delete(HOLDER);
            transport.removeDirectory(FOLDER);
        } catch (final RuntimeException ex) {
            // Reported and not raised: the sync itself has finished, and failing
            // to tidy up is not a reason to tell the tester their work did not
            // land.
            Logger.warn("The sync lock could not be released: " + ex.getMessage());
        }
    }
}
