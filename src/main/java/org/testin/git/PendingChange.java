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
 * {@code oldState} is the committed side a revert puts back: there for a
 * deletion and a modification of a test case, and absent for an addition and
 * for anything that is not a test case. The side after the change is not kept:
 * nothing but a test read it (#312, A100).
 */
public record PendingChange(@NotNull ChangeSubject subject, @NotNull String name, @NotNull String testSet, @NotNull String testCaseId, @NotNull Path relativeFilePath, @NotNull DiffType type, @Nullable TestCaseDto oldState, @NotNull List<FieldChange> fieldChanges) {

    /**
     * The case as it was committed - the side a revert puts back.
     * <p>
     * A deletion and a modification both carry it; the factory populates it for
     * exactly those. Asked here so the revert does not read the nullable field
     * and check it.
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
