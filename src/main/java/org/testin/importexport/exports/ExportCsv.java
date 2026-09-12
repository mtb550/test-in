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

package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportCsv {

    // UC-SHARE-002
    public void exportToFile(final @NotNull Project p, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile), StandardCharsets.UTF_8))) {
            final @NotNull List<String> headerNames = TestEditorAttributes.all(Can.EXPORT).stream()
                    .map(TestEditorAttributes::getName)
                    .toList();

            writer.write(String.join(",", headerNames));
            writer.newLine();

            for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                for (final TestCaseDto tc : entry.getValue()) {
                    final @NotNull List<String> rowValues = new ArrayList<>();
                    for (final TestEditorAttributes attr : TestEditorAttributes.all(Can.EXPORT)) {
                        rowValues.add(escapeCsvField(attr.gridValue(tc)));
                    }
                    writer.write(String.join(",", rowValues));
                    writer.newLine();
                }
            }
        } catch (final IOException ex) {
            Logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }

        ExportNotice.show(p, destFile);
    }

    private @NotNull String escapeCsvField(final @NotNull String value) {
        if (value.contains(",") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r")
        )
            return "\"" + value.replace("\"", "\"\"") + "\"";

        return value;
    }
}

