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
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;

import java.util.Map;
import java.util.UUID;

@FunctionalInterface
public interface ReportHandler {
    /**
     * The formats with no report generator — see {@link ExportHandler#UNSUPPORTED}.
     */
    ReportHandler UNSUPPORTED = (p, trDir, tr, detailsMap) -> {
        throw new IllegalStateException("This format has no report generator");
    };

    byte @NotNull [] execute(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap);
}
