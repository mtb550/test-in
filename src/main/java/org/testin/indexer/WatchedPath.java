package org.testin.indexer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Which test project a changed file belongs to, and whether it is test data at
 * all (#20).
 * <p>
 * The file listener sees every event in the IDE - a build writing class files, a
 * search index, somebody else's plugin - and has to answer this before it does
 * anything else. So the answer is here, as arithmetic on paths with no VFS and
 * no services in it, where it can be tested rather than watched.
 * <p>
 * A project folder rather than the file itself, because that is the unit the
 * indexer re-reads: {@code scanSingleProject} is what a pull or a hand edit
 * costs, and forty files changed in one project is one scan.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WatchedPath {

    /**
     * The test project this changed file sits in, and empty when it sits
     * somewhere Testin does not read.
     * <p>
     * Empty for four things, each for its own reason: a path outside the Testin
     * root, which is almost every event the listener will ever see; the root
     * itself, which is a folder of projects and not a project; anything under a
     * {@code .git} directory, because Git rewrites its own files constantly
     * during a pull and none of it is test data; and a root that has not been
     * configured, where there is nothing to watch yet.
     */
    public static @NotNull Optional<Path> testProjectOf(final @NotNull Path changed, final @NotNull Path root) {
        if (!org.testin.setting.TestinRoot.isConfigured(root)) return Optional.empty();
        if (!changed.startsWith(root)) return Optional.empty();

        final @NotNull Path relative = root.relativize(changed);

        // Asked of the text, not the segment count: relativizing the root
        // against itself answers an empty path, and an empty path has one name
        // rather than none - so counting segments let the root through and
        // named it as a project of its own.
        if (relative.toString().isEmpty()) return Optional.empty();
        if (isGitsOwn(relative)) return Optional.empty();

        return Optional.of(root.resolve(relative.getName(0)));
    }

    /**
     * Whether any segment of this path belongs to Git.
     * <p>
     * Any segment rather than the first, because a test project is itself a
     * repository: the {@code .git} folder sits inside the project, not beside
     * it, so checking only the top would let every pull look like test data
     * changing.
     */
    private static boolean isGitsOwn(final @NotNull Path relative) {
        for (final Path segment : relative) {
            if (ProjectIndexer.isGitsOwn(segment.toString())) return true;
        }

        return false;
    }
}
