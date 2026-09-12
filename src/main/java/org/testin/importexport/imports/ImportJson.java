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

package org.testin.importexport.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.importexport.FileTypes;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.io.File;
import java.util.*;

public class ImportJson {

    // UC-SHARE-006
    public @NotNull Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            result.putAll(parseFile(p, file));
        } catch (final Exception ex) {
            Logger.error("JSON import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("import.parse.error.format", FileTypes.JSON.getLabel()), ex.getMessage());
        }
        return result;
    }

    // UC-SHARE-005, Rule-SHARE-024
    public @NotNull Map<String, List<TestCaseDto>> parseFile(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> data = Services.getInstance(p, Mapper.class).readValue(file, new TypeReference<>() {
        });
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        for (final Map.Entry<String, List<TestCaseDto>> entry : data.entrySet()) {
            final @NotNull List<TestCaseDto> sanitized = new ArrayList<>();

            for (final TestCaseDto tc : entry.getValue()) {
                tc.setId(UUID.randomUUID());
                sanitized.add(tc);
            }

            if (!sanitized.isEmpty()) {
                result.put(entry.getKey(), sanitized);
            }
        }
        return result;
    }
}