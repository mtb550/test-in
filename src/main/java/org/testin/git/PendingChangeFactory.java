package org.testin.git;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns one changed file into the change the review shows.
 * <p>
 * What the file is decides how it is read, and the decision is made here rather
 * than by whoever renders a row. It used to read every {@code .json} as a test
 * case, which is how a test run became a nameless row and an edited run became
 * no row at all (#66).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PendingChangeFactory {

    private static final @NotNull String JSON = ".json";

    /**
     * The change for one file. Always one: a file Git reports as changed is a
     * file the tester has to be able to commit, and the review is the only place
     * that can offer it - answering null here used to drop a run, a reorder or
     * an audit stamp out of the commit entirely (#66).
     */
    static @NotNull PendingChange fromFile(
            final @NotNull DiffType type,
            final @Nullable String beforeJson,
            final @Nullable String afterJson,
            final @NotNull Path relativePath,
            final @NotNull Mapper mapper) {
        return switch (subjectOf(relativePath, afterJson == null ? beforeJson : afterJson, mapper)) {
            case TEST_CASE -> testCase(type, beforeJson, afterJson, relativePath, mapper);
            case TEST_RUN -> testRun(type, beforeJson, afterJson, relativePath, mapper);
            case MARKER -> marker(type, beforeJson, afterJson, relativePath, mapper);
            case OTHER -> other(type, relativePath);
        };
    }

    /**
     * What the file is, read from what is in it.
     * <p>
     * A marker is the dotfile that makes a directory a node - {@code .tp},
     * {@code .ts}, {@code .tr} and the rest - and that is a naming rule the
     * plugin owns, so the name settles it. For everything else the content
     * decides: a run carries {@code results}, a test case carries a description
     * and an expected result. Reading the file rather than trusting its name is
     * what keeps a renamed or hand-placed file from being taken for something it
     * is not - which is the mistake this whole class was written to stop.
     * <p>
     * When the content cannot be read at all, the name is the fallback: the
     * indexer writes a test case as {@code <id>.json}. Anything left is a file
     * nobody planned for, and it is still listed - what the review does not show
     * cannot be committed.
     */
    private static @NotNull ChangeSubject subjectOf(final @NotNull Path relativePath, final @Nullable String json,
                                                    final @NotNull Mapper mapper) {
        final String fileName = relativePath.getFileName().toString();

        if (fileName.startsWith(".")) return ChangeSubject.MARKER;
        if (!fileName.endsWith(JSON)) return ChangeSubject.OTHER;

        final Map<String, Object> fields = fieldsIn(mapper, json);
        if (fields.containsKey("results")) return ChangeSubject.TEST_RUN;
        if (fields.containsKey("description") || fields.containsKey("expectedResult")) return ChangeSubject.TEST_CASE;

        if (!fields.isEmpty()) return ChangeSubject.OTHER;

        return isTestCaseId(fileName.substring(0, fileName.length() - JSON.length()))
                ? ChangeSubject.TEST_CASE
                : ChangeSubject.OTHER;
    }

    /**
     * The JSON as a plain map, or empty when there is nothing readable there.
     * Used to ask what a file is before committing to a type for it.
     */
    private static @NotNull Map<String, Object> fieldsIn(final @NotNull Mapper mapper, final @Nullable String json) {
        if (json == null || json.isBlank()) return Map.of();

        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (final RuntimeException unreadable) {
            return Map.of();
        }
    }

    private static boolean isTestCaseId(final @NotNull String name) {
        try {
            UUID.fromString(name);
            return true;
        } catch (final IllegalArgumentException notAnId) {
            return false;
        }
    }

    private static @NotNull PendingChange testCase(
            final @NotNull DiffType type, final @Nullable String beforeJson, final @Nullable String afterJson,
            final @NotNull Path relativePath, final @NotNull Mapper mapper) {
        final String testSet = parentName(relativePath);

        return switch (type) {
            case ADDED -> {
                final TestCaseDto newState = read(mapper, afterJson, TestCaseDto.class);
                yield new PendingChange(ChangeSubject.TEST_CASE, newState.getDescription(), testSet,
                        newState.getId().toString(), relativePath, DiffType.ADDED, null, newState,
                        List.of(new FieldChange("Test Case", "", newState.getDescription(), ChangeType.CREATE_TEST_CASE)));
            }
            case DELETED -> {
                final TestCaseDto oldState = read(mapper, beforeJson, TestCaseDto.class);
                yield new PendingChange(ChangeSubject.TEST_CASE, oldState.getDescription(), testSet,
                        oldState.getId().toString(), relativePath, DiffType.DELETED, oldState, null,
                        List.of(new FieldChange("Test Case", oldState.getDescription(), "", ChangeType.REMOVE_TEST_CASE)));
            }
            case MODIFIED -> {
                final TestCaseDto oldState = read(mapper, beforeJson, TestCaseDto.class);
                final TestCaseDto newState = read(mapper, afterJson, TestCaseDto.class);
                final List<FieldChange> fieldChanges = TestCaseChangeComparator.compare(oldState, newState);

                // A test case file that changed with no reviewable field
                // different - a reordering, an audit stamp - is still a change
                // to commit, so it gets the row it needs to be selected on.
                yield new PendingChange(ChangeSubject.TEST_CASE, newState.getDescription(), testSet,
                        newState.getId().toString(), relativePath, DiffType.MODIFIED, oldState, newState,
                        fieldChanges.isEmpty()
                                ? List.of(new FieldChange("Test Case", "", "reordered or restamped", ChangeType.CHANGE_FILE))
                                : fieldChanges);
            }
        };
    }

    private static @NotNull PendingChange testRun(
            final @NotNull DiffType type, final @Nullable String beforeJson, final @Nullable String afterJson,
            final @NotNull Path relativePath, final @NotNull Mapper mapper) {
        final String runName = parentName(relativePath);

        final List<FieldChange> changes = switch (type) {
            case ADDED -> List.of(new FieldChange("Test Run", "", summary(read(mapper, afterJson, TestRunDto.class)),
                    ChangeType.CREATE_TEST_RUN));
            case DELETED -> List.of(new FieldChange("Test Run", summary(read(mapper, beforeJson, TestRunDto.class)), "",
                    ChangeType.REMOVE_TEST_RUN));
            case MODIFIED -> TestRunChangeComparator.compare(
                    read(mapper, beforeJson, TestRunDto.class), read(mapper, afterJson, TestRunDto.class));
        };

        return new PendingChange(ChangeSubject.TEST_RUN, runName, "", "", relativePath, type, null, null, changes);
    }

    /**
     * A marker change, described by the one thing in it a tester recognizes:
     * its status. Everything else it holds is the audit the plugin fills in.
     */
    private static @NotNull PendingChange marker(
            final @NotNull DiffType type, final @Nullable String beforeJson, final @Nullable String afterJson,
            final @NotNull Path relativePath, final @NotNull Mapper mapper) {
        final String node = parentName(relativePath);
        final String before = statusIn(mapper, beforeJson);
        final String after = statusIn(mapper, afterJson);

        final ChangeType changeType = switch (type) {
            case ADDED -> ChangeType.CREATE_MARKER;
            case DELETED -> ChangeType.REMOVE_MARKER;
            case MODIFIED -> ChangeType.CHANGE_MARKER;
        };

        return new PendingChange(ChangeSubject.MARKER, node, "", "", relativePath, type, null, null,
                List.of(new FieldChange(relativePath.getFileName().toString(), before, after, changeType)));
    }

    /**
     * The row for a file that could not be read at all - deleted between the
     * status and the read, or written by something else. It says only what Git
     * said, which is enough to select it and commit it.
     */
    static @NotNull PendingChange unreadable(final @NotNull DiffType type, final @NotNull Path relativePath) {
        return other(type, relativePath);
    }

    private static @NotNull PendingChange other(final @NotNull DiffType type, final @NotNull Path relativePath) {
        final ChangeType changeType = switch (type) {
            case ADDED -> ChangeType.CREATE_FILE;
            case DELETED -> ChangeType.REMOVE_FILE;
            case MODIFIED -> ChangeType.CHANGE_FILE;
        };

        return new PendingChange(ChangeSubject.OTHER, relativePath.getFileName().toString(), "", "",
                relativePath, type, null, null,
                List.of(new FieldChange(relativePath.toString(), "", "", changeType)));
    }

    /**
     * What a run holds, in one line: how many cases and how they stand.
     */
    private static @NotNull String summary(final @NotNull TestRunDto run) {
        return TestRunChangeComparator.verdictSummary(run);
    }

    /**
     * The status inside a marker, or blank when the file is not there or does
     * not carry one. Read as a map because seven marker classes hold different
     * statuses and this needs the word, not the type.
     */
    private static @NotNull String statusIn(final @NotNull Mapper mapper, final @Nullable String json) {
        final Object status = fieldsIn(mapper, json).get("status");
        return status == null ? "" : status.toString();
    }

    private static @NotNull String parentName(final @NotNull Path relativePath) {
        final Path parent = relativePath.getParent();
        return parent == null ? "" : parent.getFileName().toString();
    }

    private static <T> @NotNull T read(final @NotNull Mapper mapper, final @Nullable String json,
                                       final @NotNull Class<T> type) {
        if (json == null) throw new IllegalStateException("Missing Git file revision");
        return mapper.readValue(json, type);
    }
}
