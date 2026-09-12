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

package org.testin.model;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;

import java.util.function.Consumer;

@FunctionalInterface
public interface RemoveHandler {

    /**
     * Removes the node, then reports whether it went. The callback is what makes
     * the ordering safe: the VFS delete is asynchronous, and the indexer cache
     * must only be updated once it has happened (see CLAUDE.md - the other order
     * leaves phantom directories behind).
     * <p>
     * It always runs, so a caller counting completions is never left waiting, and
     * its argument says which kind of completion it was: false for a deletion the
     * VFS refused, and for the fixed containers that are never removed at all.
     */
    void remove(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull Consumer<@NotNull Boolean> onRemoved);
}
