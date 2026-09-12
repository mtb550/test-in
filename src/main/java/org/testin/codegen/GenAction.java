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

package org.testin.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface GenAction {
    void execute(final @NotNull Project p, final @NotNull Object obj);

    /**
     * The same, for many at once. One at a time unless a generator can do
     * better - which the test method generator can, because a whole set is one
     * class and the class only needs finding and formatting once.
     */
    default void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        for (final Object item : items) execute(p, item);
    }
}