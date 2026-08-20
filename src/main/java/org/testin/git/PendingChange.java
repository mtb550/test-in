package org.testin.git;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.List;

/**
 * One reviewable change in the repository, whatever kind of file it is about.
 * <p>
 * It was {@code TestCaseDiff} and knew only test cases. That is why a test run
 * appeared as a nameless case, and an edited one appeared not at all (#66).
 * <p>
 * The subject says what the change is about. The name and the test set are
 * carried here rather than dug out of a test case that may not exist: a run has
 * a name and no test set, and a marker names the node it belongs to.
 * <p>
 * Exactly one side is absent for a pure add or delete: {@code oldState} is null
 * for {@link DiffType#ADDED}, {@code newState} is null for
 * {@link DiffType#DELETED}. Both are null for anything that is not a test case.
 */
public record PendingChange(@NotNull ChangeSubject subject, @NotNull String name, @NotNull String testSet,
                            @NotNull String testCaseId, @NotNull Path relativeFilePath, @NotNull DiffType type,
                            @Nullable TestCaseDto oldState, @Nullable TestCaseDto newState,
                            @NotNull List<FieldChange> fieldChanges) {

    /**
     * The test case this change is about: the side of it that exists.
     * <p>
     * A deletion is about the case that was there; everything else is about the
     * case that is there now. The factory always populates that side for a test
     * case: an addition reads the new revision, a deletion the old one, a
     * modification both.
     * <p>
     * So the question has an answer for every test-case change. Asking it here
     * keeps the two nullable fields from spreading a null check across
     * everything that renders a row.
     *
     * @throws IllegalStateException if asked of a change that is not about a
     *                               test case, or of one built without the side
     *                               its own type says it must have
     */
    public @NotNull TestCaseDto testCase() {
        if (subject != ChangeSubject.TEST_CASE) {
            throw new IllegalStateException("A " + subject + " change is not about a test case: " + relativeFilePath);
        }

        final TestCaseDto state = type == DiffType.DELETED ? oldState : newState;
        if (state == null) {
            throw new IllegalStateException("A " + type + " change carries no test case: " + relativeFilePath);
        }
        return state;
    }

    /**
     * The case as it was committed - the side a revert puts back.
     * <p>
     * A deletion and a modification both carry it; the factory populates it for
     * exactly those. Asked here so the revert does not read the nullable field
     * and check it, which is the same reason {@link #testCase()} exists.
     *
     * @throws IllegalStateException if asked of a change that never had a
     *                               committed side
     */
    public @NotNull TestCaseDto committedState() {
        if (oldState == null) {
            throw new IllegalStateException("A " + type + " change carries no committed state: " + relativeFilePath);
        }
        return oldState;
    }

    /**
     * Whether a row of this change can be put back. Only a test case can: the
     * revert writes a test case through the indexer, and a run or a marker has
     * no field-level revert to apply.
     */
    public boolean isRevertible() {
        return subject == ChangeSubject.TEST_CASE;
    }
}
