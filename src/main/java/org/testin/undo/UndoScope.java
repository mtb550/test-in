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

package org.testin.undo;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Whose history a CTRL+Z is asking about.
 * <p>
 * One per surface rather than one for the plugin: the tree keeps its own, and
 * every test editor keeps its own. Two editors open, two cases removed in each,
 * and CTRL+Z in either brings back the two that were removed there - which is
 * what a tester means by the key, and what the IDE's own editors already do.
 * <p>
 * An editor is keyed by the test set it shows rather than by the instance,
 * because every write path already has that path in its hand and none of them
 * has the editor. Closing an editor and opening it again on the same set is the
 * same history, which is also the right answer.
 */
public record UndoScope(@NotNull String key) {

    /**
     * The project tree: nodes moved, renamed and removed.
     */
    public static final @NotNull UndoScope TREE = new UndoScope("tree");

    /**
     * UC-INTERNAL-005, Rule-INTERNAL-045.
     * <p>
     * The editor showing this test set.
     */
    public static @NotNull UndoScope of(final @NotNull Path testSetPath) {
        return new UndoScope(testSetPath.toString());
    }
}
