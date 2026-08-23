package org.testin.indexer;

import com.intellij.openapi.components.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.APP)
public final class OwnWrites {

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
     * Kept as text rather than as {@link Path}, because the event side and the
     * write side spell the same file differently often enough - one from the
     * VFS, one from a nio path - and normalizing both to a string once is
     * cheaper than trusting them to agree.
     */
    private final @NotNull Map<String, Long> written = new ConcurrentHashMap<>();

    public void record(final @NotNull Path path) {
        forgetOldEntries();
        written.put(key(path), System.currentTimeMillis());
    }

    /**
     * Whether this file changed because the plugin changed it.
     */
    public boolean areOurs(final @NotNull Path path) {
        return Optional.ofNullable(written.get(key(path)))
                .filter(at -> System.currentTimeMillis() - at < SETTLES_IN_MILLIS)
                .isPresent();
    }

    /**
     * Dropped on the way in rather than on a timer: this map is only ever
     * touched by a write, so a plugin that stops writing stops needing to be
     * swept.
     */
    private void forgetOldEntries() {
        final long now = System.currentTimeMillis();
        written.values().removeIf(at -> now - at >= SETTLES_IN_MILLIS);
    }

    private static @NotNull String key(final @NotNull Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
