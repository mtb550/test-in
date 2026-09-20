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
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ImportExcel {
    // UC-SHARE-006
    public @NotNull Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>(parseFile(p, file));

        Logger.info("Import: parsed " + result.values().stream().mapToInt(List::size).sum()
                + " cases from " + result.size() + " sheet(s) of " + file.getName());
        return result;
    }

    public @NotNull Map<String, List<TestCaseDto>> parseFile(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try (InputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            parseWorkbook(workbook, p, result);

        } catch (final IOException ex) {
            Logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }
        return result;
    }

    // UC-SHARE-005, Rule-SHARE-106
    private void parseWorkbook(final @NotNull Workbook workbook, final @NotNull Project p, final @NotNull Map<String, List<TestCaseDto>> result) {
        final @NotNull DataFormatter dataFormatter = new DataFormatter();
        int refused = 0;

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) continue;

            final @NotNull Sheet sheet = workbook.getSheetAt(i);
            final @NotNull Parsed parsed = parseSheet(p, sheet, dataFormatter);
            refused += parsed.refused();

            if (!parsed.cases().isEmpty()) {
                result.put(sheet.getSheetName(), parsed.cases());
            }
        }

        // Rule-SHARE-106
        TestEditorAttributes.sayWhatWasRefused(p, refused);
    }

    private record Parsed(@NotNull List<TestCaseDto> cases, int refused) {
        private static final @NotNull Parsed NOTHING = new Parsed(List.of(), 0);
    }

    private @NotNull Parsed parseSheet(final @NotNull Project p, final @NotNull Sheet sheet, final @NotNull DataFormatter dataFormatter) {
        return Optional.ofNullable(sheet.getRow(0))
                .map(headerRow -> readRows(p, sheet, headerRow, dataFormatter))
                .orElse(Parsed.NOTHING);
    }

    private @NotNull Parsed readRows(final @NotNull Project p, final @NotNull Sheet sheet, final @NotNull Row headerRow, final @NotNull DataFormatter dataFormatter) {
        final @NotNull Map<String, Integer> headerIndexMap = new HashMap<>();
        for (final Cell cell : headerRow) {
            final @NotNull String headerName = dataFormatter.formatCellValue(cell).trim();
            for (final TestEditorAttributes reqCol : TestEditorAttributes.all(Can.IMPORT)) {
                if (reqCol.isColumn(headerName)) {
                    headerIndexMap.put(reqCol.getName().toLowerCase(), cell.getColumnIndex());
                }
            }
        }

        final @NotNull List<TestCaseDto> sheetList = new ArrayList<>();

        int refused = 0;

        for (final Row row : sheet) {
            if (row.getRowNum() == headerRow.getRowNum() || isEmpty(row, dataFormatter)) continue;

            final @NotNull TestCaseDto currentTestCase = new TestCaseDto().setId(UUID.randomUUID());

            refused += TestEditorAttributes.importRow(p, currentTestCase, attr -> Optional.ofNullable(headerIndexMap.get(attr.getName().toLowerCase()))
                    .map(colIndex -> dataFormatter.formatCellValue(row.getCell(colIndex)).trim())
                    .orElse(""));

            sheetList.add(currentTestCase);
        }

        return new Parsed(sheetList, refused);
    }

    private static boolean isEmpty(final @NotNull Row row, final @NotNull DataFormatter dataFormatter) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            if (!dataFormatter.formatCellValue(row.getCell(c)).trim().isEmpty()) return false;
        }
        return true;
    }
}
