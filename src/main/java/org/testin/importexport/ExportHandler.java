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

package org.testin.importexport;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.io.File;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ExportHandler {
    /**
     * The formats that cannot be exported to. A value rather than a null, so
     * {@link org.testin.importexport.FileTypes} holds a handler either way and the
     * dropdowns ask what a format supports instead of whether one exists.
     */
    ExportHandler UNSUPPORTED = (p, destFile, sheetsData) -> {
        throw new IllegalStateException("This format cannot be exported to");
    };

    void execute(final @NotNull Project p, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData);
}
