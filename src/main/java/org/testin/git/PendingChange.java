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
 * {@code committed} is the test case as it was committed, the side a revert
 * puts back: a deletion and a modification of a test case carry it. An addition
 * and anything that is not a test case have no committed side, and carry an
 * empty test case there that nothing reads - the type already says there is
 * nothing to put back. It was null for those, behind an accessor that threw,
 * so every reader had to know when it was allowed to ask (#66, finding 286).
 * The side after the change is not kept: nothing but a test read it (#312,
 * A100).
 */
public record PendingChange(@NotNull ChangeSubject subject, @NotNull String name, @NotNull String testSet, @NotNull String testCaseId, @NotNull Path relativeFilePath, @NotNull DiffType type, @NotNull TestCaseDto committed, @NotNull List<FieldChange> fieldChanges) {

    /**
     * Whether a row of this change can be put back. Only a test case can: the
     * revert writes a test case through the indexer, and a run or a marker has
     * no field-level revert to apply.
     */
    public boolean isRevertible() {
        return subject == ChangeSubject.TEST_CASE;
    }
}
