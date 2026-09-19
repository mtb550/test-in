/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.git;

import org.testin.model.DirectoryType;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.markers.TestRunMarker;

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
     * UC-SHARE-010, Rule-SHARE-047.
     * <p>
     * The change for one file. Always one: a file Git reports as changed is a
     * file the tester has to be able to commit, and the review is the only place
     * that can offer it - answering null here used to drop a run, a reorder or
     * an audit stamp out of the commit entirely (#66).
     */
    static @NotNull PendingChange fromFile(final @NotNull DiffType type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull Mapper mapper, final @NotNull Function<UUID, Optional<TestCaseDto>> cases) {
        return switch (subjectOf(relativePath, afterJson.isEmpty() ? beforeJson : afterJson, mapper)) {
            case TEST_CASE -> testCase(type, beforeJson, afterJson, relativePath, mapper);
            case RUN_ITEM -> runItem(type, beforeJson, afterJson, relativePath, mapper, cases);
            case TEST_RUN -> testRun(type, beforeJson, afterJson, relativePath, mapper);
            case MARKER -> marker(type, beforeJson, afterJson, relativePath, mapper);
            case OTHER -> other(type, relativePath);
        };
    }

    /**
     * What the file is, read from what is in it.
     * <p>
     * The name settles it, through {@link FileKind}: a marker is one of the seven
     * fixed names, a test case is a {@code .tc}. It used to read the file and look
     * for a field - a run carried {@code results}, a case a description - because
     * nothing in a name said what a file was, and a hand-placed file could be
     * taken for something it is not. The names say it now (#305).
     * <p>
     * A run's results are still one file whose name says nothing, so that half
     * keeps reading the content until the run becomes one file per item. Anything
     * left is a file nobody planned for, and it is still listed - what the review
     * does not show cannot be committed.
     */
    private static @NotNull ChangeSubject subjectOf(final @NotNull Path relativePath, final @NotNull String json, final @NotNull Mapper mapper) {
        final @NotNull String fileName = relativePath.getFileName().toString();

        if (FileKind.of(relativePath) == FileKind.MARKER) return ChangeSubject.MARKER;
        if (FileKind.of(relativePath) == FileKind.TEST_CASE) return ChangeSubject.TEST_CASE;
        if (FileKind.of(relativePath) == FileKind.RUN_ITEM) return ChangeSubject.RUN_ITEM;
        if (!fileName.endsWith(JSON)) return ChangeSubject.OTHER;

        return fieldsIn(mapper, json).containsKey("results") ? ChangeSubject.TEST_RUN : ChangeSubject.OTHER;
    }

    /**
     * The JSON as a plain map, or empty when there is nothing readable there.
     * Used to ask what a file is before committing to a type for it.
     */
    private static @NotNull Map<String, Object> fieldsIn(final @NotNull Mapper mapper, final @NotNull String json) {
        if (json.isBlank()) return Map.of();

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

    private static @NotNull PendingChange testCase(final @NotNull DiffType type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull Mapper mapper) {
        final @NotNull String testSet = parentName(relativePath);

        return switch (type) {
            case ADDED -> {
                final @NotNull TestCaseDto newState = read(mapper, afterJson, TestCaseDto.class);
                yield new PendingChange(ChangeSubject.TEST_CASE, newState.getDescription(), testSet,
                        newState.getId().toString(), relativePath, DiffType.ADDED, nothingCommitted(),
                        List.of(new FieldChange(Bundle.message("caption.test.case"), "", newState.getDescription(), ChangeType.CREATE_TEST_CASE)));
            }
            case DELETED -> {
                final @NotNull TestCaseDto oldState = read(mapper, beforeJson, TestCaseDto.class);
                yield new PendingChange(ChangeSubject.TEST_CASE, oldState.getDescription(), testSet,
                        oldState.getId().toString(), relativePath, DiffType.DELETED, oldState,
                        List.of(new FieldChange(Bundle.message("caption.test.case"), oldState.getDescription(), "", ChangeType.REMOVE_TEST_CASE)));
            }
            case MODIFIED -> {
                final @NotNull TestCaseDto oldState = read(mapper, beforeJson, TestCaseDto.class);
                final @NotNull TestCaseDto newState = read(mapper, afterJson, TestCaseDto.class);
                final @NotNull List<FieldChange> fieldChanges = TestCaseChangeComparator.compare(oldState, newState);

                // A test case file that changed with no reviewable field
                // different - a reordering, an audit stamp - is still a change
                // to commit, so it gets the row it needs to be selected on.
                yield new PendingChange(ChangeSubject.TEST_CASE, newState.getDescription(), testSet,
                        newState.getId().toString(), relativePath, DiffType.MODIFIED, oldState,
                        fieldChanges.isEmpty()
                                ? List.of(new FieldChange(Bundle.message("caption.test.case"), "", Bundle.message("git.change.reordered"), ChangeType.CHANGE_FILE))
                                : fieldChanges);
            }
        };
    }

    /**
     * UC-SHARE-010, Rule-SHARE-047.
     * <p>
     * One case's result in one run, named by the case rather than by the file:
     * {@code 4fd2a19b-….ri} says nothing to a tester, and the description of the
     * case it is about says everything (#305, S22). The test set beside it comes
     * from the same place, so a result reads where its case reads.
     * <p>
     * The case is asked of the index, which is the one thing that knows it - a
     * result holds the verdict, not the case. A case this repository's project
     * does not hold, or one removed since, leaves the id in its place: a row a
     * tester can still select and commit says more than no row at all.
     */
    private static @NotNull PendingChange runItem(final @NotNull DiffType type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull Mapper mapper, final @NotNull Function<UUID, Optional<TestCaseDto>> cases) {
        final @NotNull Optional<UUID> caseId = FileKind.RUN_ITEM.idIn(relativePath);
        final @NotNull Optional<TestCaseDto> tc = caseId.flatMap(cases);

        final @NotNull String name = tc.map(TestCaseDto::getDescription).filter(description -> !description.isBlank())
                .orElseGet(() -> String.valueOf(relativePath.getFileName()));
        final @NotNull String testSet = tc.map(TestCaseDto::getParent).map(DirectoryDto::getName).orElse("");

        final @NotNull List<FieldChange> changes = switch (type) {
            case ADDED -> List.of(new FieldChange(parentName(relativePath), "",
                    RunItemChangeComparator.summary(read(mapper, afterJson, TestRunItems.class)), ChangeType.CREATE_RUN_ITEM));
            case DELETED -> List.of(new FieldChange(parentName(relativePath),
                    RunItemChangeComparator.summary(read(mapper, beforeJson, TestRunItems.class)), "", ChangeType.REMOVE_RUN_ITEM));
            case MODIFIED -> RunItemChangeComparator.compare(
                    read(mapper, beforeJson, TestRunItems.class), read(mapper, afterJson, TestRunItems.class));
        };

        return new PendingChange(ChangeSubject.RUN_ITEM, name, testSet, caseId.map(UUID::toString).orElse(""),
                relativePath, type, nothingCommitted(), changes);
    }

    private static @NotNull PendingChange testRun(final @NotNull DiffType type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull Mapper mapper) {
        final @NotNull String runName = parentName(relativePath);

        final @NotNull List<FieldChange> changes = switch (type) {
            case ADDED -> List.of(new FieldChange(DirectoryType.TR.getDescription(), "", summary(read(mapper, afterJson, TestRunDto.class)),
                    ChangeType.CREATE_TEST_RUN));
            case DELETED -> List.of(new FieldChange(DirectoryType.TR.getDescription(), summary(read(mapper, beforeJson, TestRunDto.class)), "",
                    ChangeType.REMOVE_TEST_RUN));
            case MODIFIED -> TestRunChangeComparator.compare(
                    read(mapper, beforeJson, TestRunDto.class), read(mapper, afterJson, TestRunDto.class));
        };

        return new PendingChange(ChangeSubject.TEST_RUN, runName, "", "", relativePath, type, nothingCommitted(), changes);
    }

    /**
     * A marker change, described by the one thing in it a tester recognizes:
     * its status. Everything else it holds is the audit the plugin fills in.
     */
    private static @NotNull PendingChange marker(final @NotNull DiffType type, final @NotNull String beforeJson, final @NotNull String afterJson, final @NotNull Path relativePath, final @NotNull Mapper mapper) {
        final @NotNull String node = parentName(relativePath);
        final @NotNull String before = statusIn(mapper, beforeJson);
        final @NotNull String after = statusIn(mapper, afterJson);

        final @NotNull ChangeType changeType = switch (type) {
            case ADDED -> ChangeType.CREATE_MARKER;
            case DELETED -> ChangeType.REMOVE_MARKER;
            case MODIFIED -> ChangeType.CHANGE_MARKER;
        };

        final @NotNull List<FieldChange> changes = new ArrayList<>();
        changes.add(new FieldChange(relativePath.getFileName().toString(), before, after, changeType));

        // A test run's own facts live in its marker, so a .tr that changed says
        // which of them did - the configuration, the execution - the way a test
        // case's file says which of its fields changed (#305, D6).
        if (type == DiffType.MODIFIED && DirectoryType.byMarker(relativePath.getFileName().toString()).filter(kind -> kind == DirectoryType.TR).isPresent()) {
            changes.addAll(TestRunChangeComparator.compareFacts(
                    read(mapper, beforeJson, TestRunMarker.class), read(mapper, afterJson, TestRunMarker.class)));
        }

        return new PendingChange(ChangeSubject.MARKER, node, "", "", relativePath, type, nothingCommitted(), changes);
    }

    /**
     * UC-SHARE-010.
     * <p>
     * The row for a file that could not be read at all - deleted between the
     * status and the read, or written by something else. It says only what Git
     * said, which is enough to select it and commit it.
     */
    static @NotNull PendingChange unreadable(final @NotNull DiffType type, final @NotNull Path relativePath) {
        return other(type, relativePath);
    }

    private static @NotNull PendingChange other(final @NotNull DiffType type, final @NotNull Path relativePath) {
        final @NotNull ChangeType changeType = switch (type) {
            case ADDED -> ChangeType.CREATE_FILE;
            case DELETED -> ChangeType.REMOVE_FILE;
            case MODIFIED -> ChangeType.CHANGE_FILE;
        };

        return new PendingChange(ChangeSubject.OTHER, relativePath.getFileName().toString(), "", "",
                relativePath, type, nothingCommitted(),
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
    private static @NotNull String statusIn(final @NotNull Mapper mapper, final @NotNull String json) {
        final @NotNull Object status = fieldsIn(mapper, json).get("status");
        return Objects.toString(status, "");
    }

    private static @NotNull String parentName(final @NotNull Path relativePath) {
        // A file at the repository's root has no parent - the test project's own
        // marker is one - so the Optional starts at the call, not after a
        // @NotNull local that said otherwise (#66, finding 265).
        return Optional.ofNullable(relativePath.getParent()).map(Path::getFileName).map(Path::toString).orElse("");
    }

    private static <T> @NotNull T read(final @NotNull Mapper mapper, final @NotNull String json, final @NotNull Class<T> type) {
        if (json.isEmpty()) throw new IllegalStateException("Missing Git file revision");
        return mapper.readValue(json, type);
    }

    /**
     * What a change with no committed side carries there: an empty test case,
     * which nothing reads, because the change's type already says there is
     * nothing to put back (#66, finding 286).
     */
    private static @NotNull TestCaseDto nothingCommitted() {
        return TestCaseDto.builder().build();
    }
}
