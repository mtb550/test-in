package org.testin.indexer;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.SystemInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the plugin itself just wrote (#20).
 * <p>
 * The file listener cannot tell a tester's hand edit from the plugin saving a
 * test case - both arrive as the same VFS event. Without this, every save the
 * plugin makes would re-read the project and rebuild the tree underneath the
 * tester who made it: correct, and unusable.
 * <p>
 * Recorded at the two places the plugin touches disk, so a write path added
 * later is covered by going through them, which the architecture already
 * requires.
 */
// Package-private rather than private, the same reason DestinationForm's
// naming rule is: telling our own write from a tester's edit is the whole of
// what this class decides, and it can be asked directly with two files and no
// IDE behind it. Still not constructible from outside the indexer, which is
// what the service level is for.
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Service(Service.Level.APP)
public final class OwnWrites {

    /** A claim with no content behind it yet - a write in flight, or a delete. */
    private static final byte[] NOTHING_TO_COMPARE = new byte[0];

    /**
     * How long a path stays ours after we write it.
     * <p>
     * VFS events do not arrive with the write; they arrive when the IDE next
     * refreshes, which is a moment later and not a fixed one. Long enough to
     * cover that, short enough that a tester who really does edit the same file
     * by hand straight afterward is not ignored.
     */
    private static final long SETTLES_IN_MILLIS = 5_000;

    /**
     * One claim: when it was made, and what the plugin left on disk.
     * <p>
     * The bytes are empty while the write is still in flight, and for the
     * operations that have no content to compare - a delete, a rename, a move.
     * Those keep the time window as their whole answer, because there is
     * nothing else to ask.
     */
    private record Claim(long at, byte @NotNull [] content) {
    }

    /**
     * Kept as text rather than as {@link Path}, because the event side and the
     * write side spell the same file differently often enough - one from the
     * VFS, one from a nio path - and normalizing both to a string once is
     * cheaper than trusting them to agree.
     */
    private final @NotNull Map<String, Claim> written = new ConcurrentHashMap<>();

    /**
     * UC-INTERNAL-003, Rule-INTERNAL-019.
     * <p>
     * Claims a path before touching it. The write has not happened yet - that is
     * the point, because the event can arrive while it is still running - so
     * there is nothing to compare against and the window answers alone.
     */
    public void record(final @NotNull Path path) {
        forgetOldEntries();
        written.put(key(path), new Claim(System.currentTimeMillis(), NOTHING_TO_COMPARE));
    }

    /**
     * UC-INTERNAL-003, Rule-INTERNAL-019, Rule-INTERNAL-064.
     * <p>
     * Says what the plugin actually left on disk, once it has.
     * <p>
     * The claim above cannot tell a second change from the first, so a tester
     * who edited the same file by hand inside the window was ignored with it -
     * their edit sat on disk, absent from the screen, until they pressed
     * Refresh (#278). Given the content, the question stops being <em>when</em>
     * this file changed and becomes <em>whether it still says what we wrote</em>,
     * which is the thing actually being asked.
     */
    public void wrote(final @NotNull Path path, final byte @NotNull [] content) {
        forgetOldEntries();
        written.put(key(path), new Claim(System.currentTimeMillis(), content));
    }

    /**
     * UC-INTERNAL-003, Rule-INTERNAL-019, Rule-INTERNAL-064.
     * <p>
     * Whether this file changed because the plugin changed it.
     * <p>
     * Read for the paths the plugin has just written and for no others, so the
     * cost falls only on files it was about to redraw anyway.
     */
    public boolean areOurs(final @NotNull Path path) {
        final @NotNull Optional<Claim> claim = Optional.ofNullable(written.get(key(path)))
                .filter(one -> System.currentTimeMillis() - one.at() < SETTLES_IN_MILLIS);

        if (claim.isEmpty()) return false;

        final byte @NotNull [] ourContent = claim.orElseThrow().content();

        // Nothing to compare: the write is still running, or the operation was a
        // delete or a rename. It is ours by construction - nobody else asked for
        // it - and the window is what bounds that.
        return ourContent.length == 0 || stillSays(path, ourContent);
    }

    /**
     * Whether the file on disk is still byte for byte what the plugin wrote.
     * <p>
     * A file that cannot be read is treated as ours: the one way that happens
     * here is a write or a delete still settling, and reporting a tester's edit
     * for a file that is not there would be worse than missing one.
     */
    private static boolean stillSays(final @NotNull Path path, final byte @NotNull [] ourContent) {
        try {
            return Arrays.equals(Files.readAllBytes(path), ourContent);
        } catch (final IOException stillSettling) {
            Logger.debug("Could not read " + path.getFileName() + " to tell our write from an edit: " + stillSettling.getMessage());
            return true;
        }
    }

    /**
     * Dropped on the way in rather than on a timer: this map is only ever
     * touched by a write, so a plugin that stops writing stops needing to be
     * swept.
     */
    private void forgetOldEntries() {
        final long now = System.currentTimeMillis();
        written.values().removeIf(one -> now - one.at() >= SETTLES_IN_MILLIS);
    }

    private static @NotNull String key(final @NotNull Path path) {
        final @NotNull String full = path.toAbsolutePath().normalize().toString();

        // On a case-insensitive file system - Windows, macOS - the event side and
        // the write side can spell the same file in different case, the drive
        // letter included, and an exact-match key then reads the plugin's own save
        // as a tester's edit and re-reads the whole project under their hands.
        // Folded to one case there, and left exactly as it is where case is real.
        return SystemInfo.isFileSystemCaseSensitive ? full : full.toLowerCase(Locale.ROOT);
    }
}
