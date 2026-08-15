package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.List;

/**
 * Builds the test-case review model from raw before/after JSON, extracted from
 * {@link GitDiffProcessor} so the diff pipeline is testable against plain file
 * contents — no IDE change list required.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseDiffFactory {

    /**
     * Returns the diff for one changed file, or {@code null} for a
     * modification with no reviewable field changes.
     */
    public static @Nullable TestCaseDiff fromJson(
            final @NotNull DiffType type,
            final @Nullable String beforeJson,
            final @Nullable String afterJson,
            final @NotNull Path relativePath,
            final @NotNull Mapper mapper) {
        return switch (type) {
            case ADDED -> {
                final TestCaseDto newState = read(mapper, afterJson);
                yield new TestCaseDiff(
                        newState.getId().toString(), relativePath, DiffType.ADDED, null, newState,
                        List.of(new FieldChange(
                                "Test Case", "", newState.getDescription(), ChangeType.CREATE_TEST_CASE)));
            }
            case DELETED -> {
                final TestCaseDto oldState = read(mapper, beforeJson);
                yield new TestCaseDiff(
                        oldState.getId().toString(), relativePath, DiffType.DELETED, oldState, null,
                        List.of(new FieldChange(
                                "Test Case", oldState.getDescription(), "", ChangeType.REMOVE_TEST_CASE)));
            }
            case MODIFIED -> {
                final TestCaseDto oldState = read(mapper, beforeJson);
                final TestCaseDto newState = read(mapper, afterJson);
                final List<FieldChange> fieldChanges = TestCaseChangeComparator.compare(oldState, newState);
                yield fieldChanges.isEmpty() ? null : new TestCaseDiff(
                        newState.getId().toString(), relativePath, DiffType.MODIFIED,
                        oldState, newState, fieldChanges);
            }
        };
    }

    private static @NotNull TestCaseDto read(final @NotNull Mapper mapper, final @Nullable String json) {
        if (json == null) throw new IllegalStateException("Missing Git file revision");
        return mapper.readValue(json, TestCaseDto.class);
    }
}
