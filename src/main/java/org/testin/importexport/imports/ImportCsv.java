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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.testcase.TestEditorAttributes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ImportCsv {
    // UC-SHARE-006
    public @NotNull Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        final @NotNull List<TestCaseDto> testCases = parseFile(p, file);
        if (!testCases.isEmpty()) {
            final @NotNull String name = file.getName().replaceAll("\\.csv$", "").replaceAll("[\\\\/*?\\[\\]]", "_");
            result.put(name, testCases);
        }
        return result;
    }

    public @NotNull List<TestCaseDto> parseFile(final @NotNull Project p, final @NotNull File file) {
        return parseCsvFile(file, p);
    }

    private @NotNull Map<String, Integer> headerIndexes(final String @NotNull [] headers) {
        final @NotNull Map<String, Integer> byName = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            final @NotNull String headerName = headers[i].trim();
            for (final TestEditorAttributes reqCol : TestEditorAttributes.all(Can.IMPORT)) {
                if (reqCol.isColumn(headerName)) byName.put(reqCol.getName().toLowerCase(), i);
            }
        }

        return byName;
    }

    private @NotNull List<TestCaseDto> parseCsvFile(final @NotNull File file, final @NotNull Project p) {
        final @NotNull List<TestCaseDto> result = new ArrayList<>();
        final @NotNull List<String[]> records = parseCsvRecords(file);

        if (records.isEmpty()) return result;

        final @NotNull Map<String, Integer> headerIndexMap = headerIndexes(records.getFirst());

        int refused = 0;

        for (int r = 1; r < records.size(); r++) {
            final String @NotNull [] values = records.get(r);

            if (Arrays.stream(values).allMatch(String::isBlank)) continue;

            final @NotNull TestCaseDto currentTestCase = new TestCaseDto().setId(UUID.randomUUID());

            refused += TestEditorAttributes.importRow(p, currentTestCase, attr -> Optional.ofNullable(headerIndexMap.get(attr.getName().toLowerCase()))
                    .filter(colIndex -> colIndex < values.length)
                    .map(colIndex -> values[colIndex].trim())
                    .orElse(""));

            result.add(currentTestCase);
        }

        TestEditorAttributes.sayWhatWasRefused(p, refused);

        return result;
    }

    private @NotNull List<String[]> parseCsvRecords(final @NotNull File file) {
        final @NotNull List<String[]> records = new ArrayList<>();
        final @NotNull List<String> fields = new ArrayList<>();
        final @NotNull StringBuilder current = new StringBuilder();

        try (PushbackReader reader = new PushbackReader(
                new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))) {
            boolean inQuotes = false;
            boolean firstChar = true;
            int ci;
            while ((ci = reader.read()) != -1) {
                final char c = (char) ci;

                if (firstChar) {
                    firstChar = false;
                    if (c == '\ufeff') continue;
                }

                if (inQuotes) {
                    if (c == '"') {
                        final int next = reader.read();
                        if (next == '"') {
                            current.append('"');
                        } else {
                            inQuotes = false;
                            if (next != -1) reader.unread(next);
                        }
                    } else {
                        current.append(c);
                    }
                } else if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else if (c == '\r' || c == '\n') {
                    if (c == '\r') {
                        final int next = reader.read();
                        if (next != '\n' && next != -1) reader.unread(next);
                    }
                    endRecord(records, fields, current);
                } else {
                    current.append(c);
                }
            }

            endRecord(records, fields, current);

        } catch (final IOException ex) {
            Logger.error("CSV parse failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
        return records;
    }

    private void endRecord(final @NotNull List<String[]> records, final @NotNull List<String> fields, final @NotNull StringBuilder current) {
        if (fields.isEmpty() && current.isEmpty()) return;

        fields.add(current.toString());
        current.setLength(0);

        if (!fields.stream().allMatch(String::isEmpty)) {
            records.add(fields.toArray(new String[0]));
        }
        fields.clear();
    }
}
