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
import org.testin.util.Bundle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * What a pending change is about.
 * <p>
 * The review used to know one answer - a test case - and read every {@code .json}
 * in the repository as one. A test run parsed that way came out as a test case
 * with no description, which is why its row showed a blank name; worse, comparing
 * two revisions of it found no test-case field different, so an edited run
 * vanished from the review entirely, and since the commit stages only what the
 * review lists, it could never be committed from here (#66).
 * <p>
 * Every changed file now says which of these it is, so nothing is read as
 * something it is not and nothing drops out.
 */
@Getter
@AllArgsConstructor
public enum ChangeSubject {

    TEST_CASE(
            Bundle.message("caption.test.case")
    ),

    TEST_RUN(
            DirectoryType.TR.getDescription()
    ),

    /**
     * The dotfile that makes a directory a node.
     * <p>
     * It carries no test data, so there is little in one to read. It is still
     * what a colleague pulling the commit needs to see the directory as a test
     * set at all. And a change to one - deactivating a project, deprecating a test
     * set - is worth committing on its own.
     */
    MARKER(
            Bundle.message("change.subject.marker")
    ),

    /**
     * Anything else that turned up in the repository. Listed rather than
     * ignored: what the review does not show cannot be committed, and a file
     * nobody accounted for is exactly the one that goes missing.
     */
    OTHER(
            Bundle.message("change.subject.file")
    );

    private final @NotNull String label;
}
