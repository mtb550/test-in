package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts IntelliJ VCS changes into the test-case review model.
 */
public final class GitDiffProcessor {

    private GitDiffProcessor() {
    }

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
                appendChange(result, change, relativePath, mapper);
            } catch (final VcsException | RuntimeException ex) {
                throw new IllegalStateException("Failed to read Git change " + relativePath, ex);
            }
        }
        return result;
    }

    private static void appendChange(
            final @NotNull List<TestCaseDiff> result,
            final @NotNull Change change,
            final @NotNull Path relativePath,
            final @NotNull Mapper mapper) throws VcsException {
        final ContentRevision before = change.getBeforeRevision();
        final ContentRevision after = change.getAfterRevision();

        switch (change.getType()) {
            case NEW -> {
                final TestCaseDto newState = read(mapper, after);
                result.add(new TestCaseDiff(
                        newState.getId().toString(), relativePath, DiffType.ADDED, null, newState,
                        List.of(new TestCaseDiff.FieldChange(
                                "Test Case", "", newState.getDescription(), ChangeType.CREATE_TEST_CASE))));
            }
            case DELETED -> {
                final TestCaseDto oldState = read(mapper, before);
                result.add(new TestCaseDiff(
                        oldState.getId().toString(), relativePath, DiffType.DELETED, oldState, null,
                        List.of(new TestCaseDiff.FieldChange(
                                "Test Case", oldState.getDescription(), "", ChangeType.REMOVE_TEST_CASE))));
            }
            case MODIFICATION, MOVED -> {
                final TestCaseDto oldState = read(mapper, before);
                final TestCaseDto newState = read(mapper, after);
                final List<TestCaseDiff.FieldChange> fieldChanges = TestCaseChangeComparator.compare(oldState, newState);
                if (!fieldChanges.isEmpty()) {
                    result.add(new TestCaseDiff(
                            newState.getId().toString(), relativePath, DiffType.MODIFIED,
                            oldState, newState, fieldChanges));
                }
            }
        }
    }

    private static @NotNull TestCaseDto read(
            final @NotNull Mapper mapper,
            final ContentRevision revision) throws VcsException {
        if (revision == null) throw new IllegalStateException("Missing Git file revision");
        return mapper.readValue(revision.getContent(), TestCaseDto.class);
    }

    private static Path pathOf(final @NotNull Change change) {
        final ContentRevision revision = change.getAfterRevision() != null
                ? change.getAfterRevision()
                : change.getBeforeRevision();
        if (revision == null) return null;
        final FilePath file = revision.getFile();
        return file == null ? null : Path.of(file.getPath()).toAbsolutePath().normalize();
    }

}
