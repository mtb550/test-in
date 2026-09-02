package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
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

    /**
     * Excel's limit on a sheet name, which POI enforces by throwing.
     */
    private static final int MAX_SHEET_NAME = 31;

    private final @NotNull ExportAction exportAction;

    /**
     * Whether this name is still free in the workbook. POI answers with no
     * sheet, and this is the one place that reads that. Case does not count:
     * Excel refuses "Login" beside "login", so neither does the lookup.
     * <p>
     * Asked in the negative because that is the only way it is ever asked - both
     * callers wrote {@code if (!hasSheet(...))} - and a question read one way at
     * every call site should be named that way.
     */
    private static boolean hasNoSheet(final @NotNull Workbook workbook, final @NotNull String name) {
        return workbook.getSheet(name) == null;
    }

    /**
     * A sheet name Excel will accept and this workbook does not already hold.
     * <p>
     * Two test sets can want the same sheet: their names differ but sanitize
     * alike - "A/B" and "A*B" both become "A_B" - and a name longer than the
     * limit is cut to it, which collapses more of them. The first one keeps the
     * name and the rest are numbered.
     * <p>
     * It used to retry with the first 28 characters and an ellipsis, which broke
     * twice over: a name shorter than 28 threw out of substring, and a longer
     * one produced the same string on every pass, so the loop never ended.
     * POI decides what is legal - it knows about the colon and the quote this
     * class never removed - and the number is what makes it unique.
     */
    static @NotNull String uniqueSheetName(final @NotNull Workbook workbook, final @NotNull String proposal) {
        final @NotNull String safe = WorkbookUtil.createSafeSheetName(proposal, '_');
        if (hasNoSheet(workbook, safe)) return safe;

        // The base is shortened by as much as the number needs, so a name
        // already at the limit still has somewhere to put it.
        for (int attempt = 2; ; attempt++) {
            final @NotNull String suffix = " (" + attempt + ")";
            final @NotNull String base = safe.substring(0, Math.min(safe.length(), MAX_SHEET_NAME - suffix.length()));
            final @NotNull String candidate = base + suffix;

            if (hasNoSheet(workbook, candidate)) return candidate;
        }
    }

    public void exportToFile(final @NotNull Project p, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try (Workbook workbook = new XSSFWorkbook()) {
            final @NotNull CellStyle headerStyle = workbook.createCellStyle();
            final @NotNull Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                final @NotNull Sheet sheet = workbook.createSheet(uniqueSheetName(workbook, entry.getKey()));

                final @NotNull Row headerRow = sheet.createRow(0);
                for (int i = 0; i < exportAction.exportAttributes.size(); i++) {
                    final @NotNull Cell cell = headerRow.createCell(i);
                    cell.setCellValue(exportAction.exportAttributes.get(i).getName());
                    cell.setCellStyle(headerStyle);
                }

                int rowIndex = 1;
                for (final TestCaseDto tc : entry.getValue()) {
                    final @NotNull Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < exportAction.exportAttributes.size(); i++) {
                        final @NotNull Cell cell = row.createCell(i);
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

