package org.testin.actions.imports;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class ImportExcel extends ImportBase {

    public ImportExcel(final @NotNull SimpleTree tree) {
        super(tree, "Import from Excel", "Import test cases from an excel file", AllIcons.FileTypes.MicrosoftWindows);
    }

    public Map<String, List<TestCaseDto>> parseFile(final @NotNull Project project, final File file) throws Exception {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try (InputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            parseWorkbook(workbook, project, result);
        }
        return result;
    }

    private void parseWorkbook(final Workbook workbook, final @NotNull Project project, final Map<String, List<TestCaseDto>> result) {
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
                for (String reqCol : IMPORT_COLUMNS) {
                    if (reqCol.equalsIgnoreCase(headerName)) {
                        headerIndexMap.put(reqCol.toLowerCase(), cell.getColumnIndex());
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
                        attr.getImportSetter().accept(project, currentTestCase, rawValue);
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
