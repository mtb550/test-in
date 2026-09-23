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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.testcase.TestEditorAttributes;
import org.testin.util.Fonts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExportExcel {
    private static final int MAX_SHEET_NAME = 31;

    private static boolean hasNoSheet(final @NotNull Workbook workbook, final @NotNull String name) {
        return workbook.getSheet(name) == null;
    }

    // UC-SHARE-002, Rule-SHARE-016
    static @NotNull String uniqueSheetName(final @NotNull Workbook workbook, final @NotNull String proposal) {
        final @NotNull String safe = WorkbookUtil.createSafeSheetName(proposal, '_');
        if (hasNoSheet(workbook, safe)) return safe;

        for (int attempt = 2; ; attempt++) {
            final @NotNull String suffix = " (" + attempt + ")";
            final @NotNull String base = safe.substring(0, Math.min(safe.length(), MAX_SHEET_NAME - suffix.length()));
            final @NotNull String candidate = base + suffix;

            if (hasNoSheet(workbook, candidate)) return candidate;
        }
    }

    // UC-SHARE-002, Rule-SHARE-012
    public void exportToFile(final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try (Workbook workbook = new XSSFWorkbook()) {
            final @NotNull CellStyle headerStyle = workbook.createCellStyle();
            final @NotNull Font headerFont = workbook.createFont();
            headerFont.setFontName(Fonts.Report.FAMILY);
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                final @NotNull Sheet sheet = workbook.createSheet(uniqueSheetName(workbook, entry.getKey()));

                final @NotNull Row headerRow = sheet.createRow(0);
                for (int i = 0; i < TestEditorAttributes.all(Can.EXPORT).size(); i++) {
                    final @NotNull Cell cell = headerRow.createCell(i);
                    cell.setCellValue(TestEditorAttributes.all(Can.EXPORT).get(i).getName());
                    cell.setCellStyle(headerStyle);
                }

                int rowIndex = 1;
                for (final TestCaseDto tc : entry.getValue()) {
                    final @NotNull Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < TestEditorAttributes.all(Can.EXPORT).size(); i++) {
                        final @NotNull Cell cell = row.createCell(i);
                        cell.setCellValue(TestEditorAttributes.all(Can.EXPORT).get(i).gridValue(tc));
                    }
                }

                for (int i = 0; i < TestEditorAttributes.all(Can.EXPORT).size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                workbook.write(fos);
            }
        } catch (final IOException ex) {
            Logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
