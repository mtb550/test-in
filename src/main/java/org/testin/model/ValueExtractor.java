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

/**
 * How a run grid cell reads its text off a run item.
 * <p>
 * Not generic any more, and not shared with the test case attributes. It was
 * {@code ValueExtractor<T>} over both, which meant every one of the eighteen
 * test-case extractors was handed a {@link Project} and not one of them read it
 * - the parameter is here for a single run extractor, the one that asks the
 * indexer for the case behind a run item.
 * <p>
 * The cost of carrying it was not an unused parameter. It made "does this test
 * case hold this text" a question only a caller holding a project could ask, so
 * the editor's search box wrote its own answer over four hand-listed fields
 * while the global search asked all eighteen attributes - and the two disagreed
 * for as long as both existed (#294). The test case side is a plain
 * {@code Function} now, and the question has one owner.
 */
@FunctionalInterface
public interface ValueExtractor {
    @NotNull String execute(final @NotNull TestRunItems item, final @NotNull Project p);
}
