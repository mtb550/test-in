package org.testin.sftp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * What a sync does about one file (#94).
 * <p>
 * Decided from three hashes: the file as it stood at the last successful
 * transfer, the file here now, and the file on the server now. That first one is
 * the whole reason this channel can do more than overwrite - a server holds one
 * copy and no history, so without a remembered ancestor there is no way to tell
 * "they changed it" from "I did", and every difference would be a fight.
 * <p>
 * An empty hash means the file is not there. A real one is never empty, so the
 * absent case needs no separate flag and no reader has to test for one.
 * <p>
 * {@link #of} is the only place the rule lives. It is total - every combination
 * of three hashes lands on exactly one constant - which is what lets the caller
 * be a loop with no branches in it.
 */
@Getter
@AllArgsConstructor
public enum TransferAction {

    /**
     * Both sides already hold the same bytes, or neither holds the file at all.
     */
    NOTHING("Already the same"),

    UPLOAD("Changed here"),

    DOWNLOAD("Changed on the server"),

    /**
     * Both sides moved since the last transfer, so nothing can be chosen without
     * losing something. A test case goes to {@code TestCaseMerge}, which settles
     * it field by field; anything else asks the tester.
     * <p>
     * A file deleted on one side and edited on the other lands here too. Which of
     * those a team meant is not a question about bytes.
     */
    RESOLVE("Changed on both sides"),

    /**
     * Gone from the server, and untouched here since it was last transferred - so
     * somebody deleted it deliberately and this machine has nothing to lose.
     */
    DELETE_LOCAL("Removed on the server"),

    DELETE_REMOTE("Removed here");

    /**
     * What the tester reads beside the path when a sync says what it is about to
     * do. Written from their side of the screen: "changed here" rather than
     * "local modification".
     */
    private final @NotNull String caption;

    /**
     * What to do about one file, from where it stood and where it stands.
     *
     * @param base   its hash at the last successful transfer, empty when it was
     *               not there - or when nothing has ever been transferred
     * @param local  its hash on this machine now, empty when it is not there
     * @param remote its hash on the server now, empty when it is not there
     */
    public static @NotNull TransferAction of(final @NotNull String base, final @NotNull String local, final @NotNull String remote) {
        // Same bytes on both sides settles it before anything else is asked, and
        // it is also how "neither side has this file" answers - two empties are
        // equal. The rest of this method therefore knows the two sides differ.
        if (local.equals(remote)) return NOTHING;

        if (base.isEmpty()) {
            // Nothing was ever transferred, so there is no ancestor to judge
            // against. One side having it and the other not is simply new.
            if (remote.isEmpty()) return UPLOAD;
            if (local.isEmpty()) return DOWNLOAD;

            return RESOLVE;
        }

        if (local.equals(base)) {
            // This machine has not touched it, so whatever the server did stands.
            return remote.isEmpty() ? DELETE_LOCAL : DOWNLOAD;
        }

        if (remote.equals(base)) {
            return local.isEmpty() ? DELETE_REMOTE : UPLOAD;
        }

        // Both moved, and not to the same place.
        return RESOLVE;
    }
}
