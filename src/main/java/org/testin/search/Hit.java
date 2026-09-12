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

package org.testin.search;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.util.Icons;

import javax.swing.*;
import java.util.Optional;

/**
 * One thing the search found, and enough to go to it (#29).
 * <p>
 * A test case and a node are one shape here on purpose. Both are somewhere in
 * the tree, both open an editor, and the only difference is whether there is a
 * row to land on once it is open - so a hit carries the node to reveal and, for
 * a case, the case to select inside it. Nothing downstream asks which kind it
 * got.
 *
 * @param node     what the tree expands to and what the editor opens. For a test
 *                 case this is the test set it lives in, because a case is not a
 *                 node of its own
 * @param testCase the row to land on, and empty for a node - which is a node
 *                 selected and nothing more
 */
public record Hit(@NotNull Icon icon, @NotNull String name, @NotNull String where, @NotNull DirectoryDto node, @NotNull Optional<TestCaseDto> testCase) {

    /**
     * A test case, shown under the test set that holds it.
     */
    public static @NotNull Hit of(final @NotNull TestCaseDto tc) {
        return new Hit(Icons.TEST_CASE, tc.getDescription(), where(tc.getParent()),
                tc.getParent(), Optional.of(tc));
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-072.
     * <p>
     * A node, shown under whatever leads to it, and drawn with the icon its own
     * type declares - so a test set, a package and a run look in the search
     * exactly as they look in the tree.
     * <p>
     * The icon is the whole of what says which kind it is. A word beside the
     * name was built and taken out again: the icon already carries it, and a
     * row is for reading the name (#66, finding 51).
     */
    public static @NotNull Hit of(final @NotNull DirectoryDto node) {
        return new Hit(node.getType().getIcon(), node.getName(), where(node), node, Optional.empty());
    }

    /**
     * The chain that leads to a node, as the tree reads it. Taken from the names
     * the scan already put on the node rather than from its path, so what the
     * search shows and what the tree shows are the same words.
     */
    private static @NotNull String where(final @NotNull DirectoryDto node) {
        return String.join(" > ", node.getPath2());
    }
}
