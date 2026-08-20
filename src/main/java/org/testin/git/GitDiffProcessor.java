package org.testin.git;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builds the test-case review model from what Git reports as changed. The
 * IDE-free diff construction lives in {@link PendingChangeFactory} and the
 * porcelain parsing in {@link GitRefs}; this class only asks Git and reads the
 * two sides of each change.
 * <p>
 * It used to ask the IDE instead - {@code ChangeListManager.getAllChanges()} -
 * and that was wrong twice over.
 * <p>
 * The IDE tracks only repositories registered as VCS roots in the open project.
 * A Testin root is deliberately a separate repository from the automation
 * project, so the change list was empty for it, and the review reported "No
 * changes" however much had changed.
 * <p>
 * And a brand-new test case is untracked, which that list never reports at all.
 * So the first commit of a new test set could not be made from the plugin under
 * any layout.
 * <p>
 * Asking Git directly also makes the read match the writes: init, add, commit,
 * remote, config, pull and push already run as Git commands.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitDiffProcessor {

    public static @NotNull List<PendingChange> getPendingChanges(
            final @NotNull Project project,
            final @NotNull Path repositoryRoot) {
        final Path root = repositoryRoot.toAbsolutePath().normalize();
        final GitRepositoryService repositories = new GitRepositoryService(project);

        return toDiffs(repositories.status(root), root,
                Services.getInstance(project, Mapper.class),
                path -> repositories.showAtHead(root, path));
    }

    /**
     * The review, built from what Git said and what is on disk.
     * <p>
     * Separated from {@link #getPendingChanges} so the whole mapping can be
     * exercised against plain status lines and real files, without an IDE or a
     * repository: which lines are ours, which side of each change is read from
     * where, and which changes are not worth showing.
     *
     * @param committedContent the file's content as committed, empty when there
     *                         is none - a new file, or no commits yet
     */
    static @NotNull List<PendingChange> toDiffs(
            final @NotNull List<String> statusLines,
            final @NotNull Path repositoryRoot,
            final @NotNull Mapper mapper,
            final @NotNull Function<String, String> committedContent) {
        final Path root = repositoryRoot.toAbsolutePath().normalize();
        final List<PendingChange> result = new ArrayList<>();

        for (final GitRefs.StatusEntry entry : GitRefs.parseStatus(statusLines)) {
            final Path relativePath = Path.of(entry.path());

            // An untracked file that is no longer there is not a pending change:
            // Git listed it a moment ago and something removed it since. Listing
            // it would offer the tester a row that cannot be staged, because
            // there is no file for "git add" to find.
            if (entry.type() == DiffType.ADDED && !Files.exists(root.resolve(relativePath))) {
                Logger.warn("Skipping " + relativePath + ": Git listed it as new, and it is gone");
                continue;
            }

            try {
                result.add(PendingChangeFactory.fromFile(
                        entry.type(),
                        entry.type() == DiffType.ADDED ? "" : committedContent.apply(entry.path()),
                        workingContent(root, relativePath, entry),
                        relativePath,
                        mapper));

            } catch (final RuntimeException ex) {
                // One unreadable file does not take the review down with it. Git
                // said this path changed, so it is a change the tester has to be
                // able to commit - it is listed with what little can be said
                // about it, and the reason goes to the log. Throwing here meant a
                // file deleted between the status and the read, or a hand-edited
                // one, emptied the whole review (#66).
                Logger.warn("Listing " + relativePath + " without detail: " + ex.getMessage());
                result.add(PendingChangeFactory.unreadable(entry.type(), relativePath));
            }
        }
        return result;
    }

    /**
     * Empty for a deletion, which by definition has no file left on disk.
     * <p>
     * Read with plain file access rather than through the VFS: this is the
     * working tree as Git just described it, and a VFS copy that has not caught
     * up would disagree with the status line that named the file. {@code git} is
     * an exempt package for exactly this kind of read.
     */
    private static @NotNull String workingContent(
            final @NotNull Path root,
            final @NotNull Path relativePath,
            final @NotNull GitRefs.StatusEntry entry) {
        if (entry.type() == DiffType.DELETED) return "";

        final Path file = root.resolve(relativePath);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            // Not fatal to the review: the change is still listed, with whatever
            // can be said about it, and the caller decides what to show. A file
            // the plugin cannot read is a file the tester still has to be able
            // to commit.
            Logger.warn("Could not read changed file " + file + ": " + ex.getMessage());
            throw new IllegalStateException("Could not read " + relativePath, ex);
        }
    }
}
