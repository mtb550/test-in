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

package org.testin.codegen.method.update;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.logger.Logger;

import java.util.List;

@AllArgsConstructor
public final class NoOpCodeUpdate implements GenAction {
    private final @NotNull String fieldName;

    // Rule-CODEGEN-003
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        Logger.info("Update " + fieldName + ": data-only field, no Java code change");
    }

    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        Logger.info("Update " + fieldName + " on " + items.size() + ": data-only field, no Java code change");
    }
}
