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

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestRunDto;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record OpenBug(@NotNull Path runPath, @NotNull TestRunItems item) {
    // UC-VIEW-PANEL-008, Rule-VIEW-PANEL-064
    public static @NotNull List<OpenBug> of(final @NotNull Map<Path, TestRunDto> runs, final @NotNull UUID caseId) {
        return runs.entrySet().stream()
                .flatMap(run -> run.getValue().resultOf(caseId)
                        .filter(FailureDetail::recordsABug)
                        .map(item -> new OpenBug(run.getKey(), item))
                        .stream())
                .sorted(Comparator.comparing((OpenBug bug) -> bug.item().getExecutedAt()).reversed())
                .toList();
    }

    public @NotNull String runName() {
        return Optional.ofNullable(runPath.getFileName()).map(Path::toString).orElse(runPath.toString());
    }
}
