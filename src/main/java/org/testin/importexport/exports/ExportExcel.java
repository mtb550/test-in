package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class ExportExcel {
    private final @NotNull ExportAction exportAction;

    public void exportToFile(final @NotNull Project p, final @NotNull File destFile,
                             final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try (Workbook workbook = new XSSFWorkbook()) {
            final CellStyle headerStyle = workbook.createCellStyle();
            final Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                String safeSheetName = entry.getKey().replaceAll("[\\\\/*?\\[\\]]", "_");
                if (safeSheetName.length() > 31) {
                    safeSheetName = safeSheetName.substring(0, 31);
                }
                while (workbook.getSheet(safeSheetName) != null) {
                    safeSheetName = safeSheetName.substring(0, 28) + "...";
                }

                final Sheet sheet = workbook.createSheet(safeSheetName);

                final Row headerRow = sheet.createRow(0);
                for (int i = 0; i < exportAction.exportAttributes.size(); i++) {
                    final Cell cell = headerRow.createCell(i);
                    cell.setCellValue(exportAction.exportAttributes.get(i).getName());
                    cell.setCellStyle(headerStyle);
                }

                int rowIndex = 1;
                for (final TestCaseDto tc : entry.getValue()) {
                    final Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < exportAction.exportAttributes.size(); i++) {
                        final Cell cell = row.createCell(i);
                        cell.setCellValue(exportAction.exportAttributes.get(i).getTestValueExtractor().execute(tc, p));
                    }
                }

                for (int i = 0; i < exportAction.exportAttributes.size(); i++) {
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

        ExportNotice.show(p, destFile);
    }
}

