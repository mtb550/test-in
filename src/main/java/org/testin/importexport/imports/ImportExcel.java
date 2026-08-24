package org.testin.importexport.imports;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@AllArgsConstructor
public class ImportExcel {
    private final @NotNull ImportAction importAction;

    public @NotNull Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            result.putAll(parseFile(p, file));
        } catch (final Exception ex) {
            Logger.error("Excel import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Excel Parse Error", ex.getMessage());
        }

        // The count the file gave, against the count the tester ticks and the
        // count the import writes. Three numbers that should agree, and did not
        // (#66, finding 24).
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

    private void parseWorkbook(final @NotNull Workbook workbook, final @NotNull Project p, final @NotNull Map<String, List<TestCaseDto>> result) {
        final @NotNull DataFormatter dataFormatter = new DataFormatter();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) continue;

            final @NotNull Sheet sheet = workbook.getSheetAt(i);
            final @NotNull List<TestCaseDto> sheetList = parseSheet(p, sheet, dataFormatter);

            if (!sheetList.isEmpty()) {
                result.put(sheet.getSheetName(), sheetList);
            }
        }
    }

    /**
     * The cases on one sheet, and none at all for a sheet with no header row -
     * an empty sheet, or one whose first row the file never wrote.
     */
    private @NotNull List<TestCaseDto> parseSheet(final @NotNull Project p, final @NotNull Sheet sheet, final @NotNull DataFormatter dataFormatter) {
        return Optional.ofNullable(sheet.getRow(0))
                .map(headerRow -> readRows(p, sheet, headerRow, dataFormatter))
                .orElseGet(List::of);
    }

    private @NotNull List<TestCaseDto> readRows(final @NotNull Project p, final @NotNull Sheet sheet, final @NotNull Row headerRow, final @NotNull DataFormatter dataFormatter) {
        final @NotNull Map<String, Integer> headerIndexMap = new HashMap<>();
        for (final Cell cell : headerRow) {
            final @NotNull String headerName = dataFormatter.formatCellValue(cell).trim();
            for (final TestEditorAttributes reqCol : importAction.importAttributes) {
                if (reqCol.getName().equalsIgnoreCase(headerName)) {
                    headerIndexMap.put(reqCol.getName().toLowerCase(), cell.getColumnIndex());
                }
            }
        }

        final @NotNull List<TestCaseDto> sheetList = new ArrayList<>();

        // The sheet's own iterator visits the rows that exist, so a file with a
        // gap in the middle needs no test for the rows that are not there.
        for (final Row row : sheet) {
            if (row.getRowNum() == headerRow.getRowNum() || isEmpty(row, dataFormatter)) continue;

            final @NotNull TestCaseDto currentTestCase = new TestCaseDto().setId(UUID.randomUUID());

            for (final TestEditorAttributes attr : TestEditorAttributes.values()) {
                if (attr.can(Can.IMPORT)) {
                    // A column the file does not carry reads as blank, which is
                    // what an absent value means to every importer.
                    final @NotNull String rawValue = Optional.ofNullable(headerIndexMap.get(attr.getName().toLowerCase()))
                            .map(colIndex -> dataFormatter.formatCellValue(row.getCell(colIndex)).trim())
                            .orElse("");
                    attr.getImportSetter().execute(p, currentTestCase, rawValue);
                }
            }

            sheetList.add(currentTestCase);
        }

        return sheetList;
    }

    /**
     * A row worth importing has something in it. The formatter answers a missing
     * cell with an empty string, so no cell needs testing for its own absence.
     */
    private static boolean isEmpty(final @NotNull Row row, final @NotNull DataFormatter dataFormatter) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            if (!dataFormatter.formatCellValue(row.getCell(c)).trim().isEmpty()) return false;
        }
        return true;
    }
}
