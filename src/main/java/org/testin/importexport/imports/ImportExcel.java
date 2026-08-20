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
        final Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            result.putAll(parseFile(p, file));
        } catch (final Exception ex) {
            Logger.error("Excel import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Excel Parse Error", ex.getMessage());
        }
        return result;
    }

    public @NotNull Map<String, List<TestCaseDto>> parseFile(final @NotNull Project p, final @NotNull File file) {
        final Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try (InputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            parseWorkbook(workbook, p, result);

        } catch (final IOException ex) {
            Logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }
        return result;
    }

    private void parseWorkbook(final @NotNull Workbook workbook, final @NotNull Project p,
                               final @NotNull Map<String, List<TestCaseDto>> result) {
        final DataFormatter dataFormatter = new DataFormatter();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) continue;

            final Sheet sheet = workbook.getSheetAt(i);
            final String sheetName = sheet.getSheetName();

            final Row headerRow = sheet.getRow(0);
            if (headerRow == null) continue;

            final Map<String, Integer> headerIndexMap = new HashMap<>();

            for (final Cell cell : headerRow) {
                final String headerName = dataFormatter.formatCellValue(cell).trim();
                for (final TestEditorAttributes reqCol : importAction.importAttributes) {
                    if (reqCol.getName().equalsIgnoreCase(headerName)) {
                        headerIndexMap.put(reqCol.getName().toLowerCase(), cell.getColumnIndex());
                    }
                }
            }

            final List<TestCaseDto> sheetList = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                final Row row = sheet.getRow(r);
                if (row == null) continue;

                boolean isRowEmpty = true;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    if (row.getCell(c) != null && !dataFormatter.formatCellValue(row.getCell(c)).trim().isEmpty()) {
                        isRowEmpty = false;
                        break;
                    }
                }
                if (isRowEmpty) continue;

                final TestCaseDto currentTestCase = new TestCaseDto().setId(UUID.randomUUID());

                for (final TestEditorAttributes attr : TestEditorAttributes.values()) {
                    if (attr.can(Can.IMPORT)) {
                        final Integer colIndex = headerIndexMap.get(attr.getName().toLowerCase());
                        final String rawValue = colIndex == null
                                ? ""
                                : dataFormatter.formatCellValue(row.getCell(colIndex)).trim();
                        attr.getImportSetter().execute(p, currentTestCase, rawValue);
                    }
                }

                sheetList.add(currentTestCase);
            }

            if (!sheetList.isEmpty()) {
                result.put(sheetName, sheetList);
            }
        }
    }
}
