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

package org.testin.indexer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeFigures;
import org.testin.model.dto.dirs.DirectoryDto;

/**
 * How one kind of node arrives at its numbers.
 * <p>
 * Named rather than a bare function so {@link org.testin.model.NodeStatistics} reads as a
 * declaration - the same reason {@code GenAction} and {@code RemoveHandler}
 * are interfaces and not {@code BiConsumer}s.
 */
@FunctionalInterface
public interface FiguresGatherer {

    @NotNull
    NodeFigures of(final @NotNull Project p, final @NotNull DirectoryDto dto);
}
