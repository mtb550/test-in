package org.testin.importExport.imports;

import com.intellij.openapi.project.Project;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ImportExcel {
    private final ImportAction importAction;

    public ImportExcel(final @NotNull ImportAction importAction) {
        this.importAction = importAction;
    }

    public Map<String, List<TestCaseDto>> processImport(final @NotNull Project p, final File file) {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try {
            Map<String, List<TestCaseDto>> parsed = parseFile(p, file);
            result.putAll(parsed);
        } catch (final Exception ex) {
            Logger.error("Excel import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Excel Parse Error", ex.getMessage());
        }
        return result;
    }

    public Map<String, List<TestCaseDto>> parseFile(final @NotNull Project p, final File file) {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try (InputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            parseWorkbook(workbook, p, result);

        } catch (final IOException ex) {
            Logger.error(ex.getMessage());
            throw new RuntimeException(ex);
        }
        return result;
    }

    private void parseWorkbook(final Workbook workbook, final @NotNull Project p, final Map<String, List<TestCaseDto>> result) {
        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) continue;

            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) continue;

            Map<String, Integer> headerIndexMap = new HashMap<>();

            for (Cell cell : headerRow) {
                String headerName = dataFormatter.formatCellValue(cell).trim();
                for (TestEditorAttributes reqCol : importAction.importAttributes) {
                    if (reqCol.getName().equalsIgnoreCase(headerName)) {
                        headerIndexMap.put(reqCol.getName().toLowerCase(), cell.getColumnIndex());
                    }
                }
            }

            List<TestCaseDto> sheetList = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
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
                currentTestCase.setNext(null);
                currentTestCase.setIsHead(null);

                for (TestEditorAttributes attr : TestEditorAttributes.values()) {
                    if (attr.isImportable()) {
                        Integer colIndex = headerIndexMap.get(attr.getName().toLowerCase());
                        String rawValue = "";
                        if (colIndex != null) {
                            Cell dataCell = row.getCell(colIndex);
                            rawValue = dataFormatter.formatCellValue(dataCell).trim();
                        }
                        attr.getImportSetter().accept(p, currentTestCase, rawValue);
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