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

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.model.dto.TestCaseDto;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportThenImportIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull ZonedDateTime CREATED = ZonedDateTime.of(2026, 8, 20, 3, 33, 24, 0, ZoneId.of("Asia/Riyadh"));
    private static final @NotNull ZonedDateTime UPDATED = ZonedDateTime.of(2026, 9, 14, 11, 30, 5, 0, ZoneId.of("Asia/Riyadh"));

    private static @NotNull TestCaseDto testCase(final @NotNull String description) {
        return TestCaseDto.builder()
                .description(description)
                .expectedResult("the dashboard opens")
                .createdBy("Sara")
                .createdAt(CREATED)
                .updatedBy("Muteb")
                .updatedAt(UPDATED)
                .build();
    }

    private @NotNull Map<String, List<TestCaseDto>> roundTrip(final @NotNull Map<String, List<TestCaseDto>> sheets) {
        final @NotNull File file = root.resolve("export.xlsx").toFile();
        FileTypes.XLSX.exportToFile(getProject(), file, sheets);

        return FileTypes.XLSX.importToFile(getProject(), file);
    }

    private static @NotNull List<String> descriptions(final @NotNull List<TestCaseDto> testCases) {
        return testCases.stream().map(TestCaseDto::getDescription).toList();
    }

    // UC-SHARE-002, Rule-SHARE-007, Rule-SHARE-012
    public void testEachTestSetIsOneSheetNamedAfterItselfWithItsRowsInOrder() {
        final @NotNull Map<String, List<TestCaseDto>> sheets = new LinkedHashMap<>();
        sheets.put("Login", List.of(testCase("log in with a valid user"), testCase("a wrong password is refused"), testCase("a locked account cannot log in")));
        sheets.put("Checkout", List.of(testCase("pay with a saved card")));

        final @NotNull Map<String, List<TestCaseDto>> read = roundTrip(sheets);

        assertEquals(List.of("Login", "Checkout"), List.copyOf(read.keySet()));
        assertEquals(descriptions(sheets.get("Login")), descriptions(read.get("Login")));
        assertEquals(descriptions(sheets.get("Checkout")), descriptions(read.get("Checkout")));
    }

    // UC-SHARE-005, Rule-SHARE-121
    public void testASheetExportedAndImportedUnchangedKeepsItsDates() {
        final @NotNull TestCaseDto read = roundTrip(Map.of("Login", List.of(testCase("log in with a valid user")))).get("Login").getFirst();

        assertEquals(CREATED.toInstant(), read.getCreatedAt().toInstant());
        assertEquals(UPDATED.toInstant(), read.getUpdatedAt().toInstant());
    }
}
