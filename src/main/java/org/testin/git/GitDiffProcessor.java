package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts IntelliJ VCS changes into the test-case review model. The IDE-free
 * diff construction lives in {@link TestCaseDiffFactory}; this class only
 * walks the change list and reads revision contents.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitDiffProcessor {

    public static @NotNull List<TestCaseDiff> getPendingChanges(
            final @NotNull Project project,
            final @NotNull Path repositoryRoot) {
        final Path normalizedRoot = repositoryRoot.toAbsolutePath().normalize();
        final Mapper mapper = Services.getInstance(project, Mapper.class);
        final List<TestCaseDiff> result = new ArrayList<>();

        for (final Change change : ChangeListManager.getInstance(project).getAllChanges()) {
            final Path filePath = pathOf(change);
            if (filePath == null || !filePath.startsWith(normalizedRoot)
                    || !filePath.toString().endsWith(".json")) {
                continue;
            }

            final Path relativePath = normalizedRoot.relativize(filePath);
            try {
                final TestCaseDiff diff = TestCaseDiffFactory.fromJson(
                        typeOf(change),
                        contentOf(change.getBeforeRevision()),
                        contentOf(change.getAfterRevision()),
                        relativePath,
                        mapper);
                if (diff != null) result.add(diff);
            } catch (final VcsException | RuntimeException ex) {
                throw new IllegalStateException("Failed to read Git change " + relativePath, ex);
            }
        }
        return result;
    }

    private static @NotNull DiffType typeOf(final @NotNull Change change) {
        return switch (change.getType()) {
            case NEW -> DiffType.ADDED;
            case DELETED -> DiffType.DELETED;
            case MODIFICATION, MOVED -> DiffType.MODIFIED;
        };
    }

    private static @Nullable String contentOf(final @Nullable ContentRevision revision) throws VcsException {
        return revision == null ? null : revision.getContent();
    }

    private static Path pathOf(final @NotNull Change change) {
        final ContentRevision revision = change.getAfterRevision() != null
                ? change.getAfterRevision()
                : change.getBeforeRevision();
        if (revision == null) return null;
        final FilePath file = revision.getFile();
        return Path.of(file.getPath()).toAbsolutePath().normalize();
    }

}
