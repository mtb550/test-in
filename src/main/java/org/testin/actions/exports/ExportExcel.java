package org.testin.actions.exports;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExportExcel extends Export {

    public ExportExcel(final @NotNull SimpleTree tree) {
        super(tree);
    }

    public void exportToFile(final @NotNull Project project, final File destFile,
                             final Map<String, List<TestCaseDto>> sheetsData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
                String safeSheetName = entry.getKey().replaceAll("[\\\\/\\*?\\[\\]]", "_");
                if (safeSheetName.length() > 31) {
                    safeSheetName = safeSheetName.substring(0, 31);
                }
                while (workbook.getSheet(safeSheetName) != null) {
                    safeSheetName = safeSheetName.substring(0, 28) + "...";
                }

                Sheet sheet = workbook.createSheet(safeSheetName);
                List<TestCaseDto> testCases = entry.getValue();

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < EXPORT_COLUMNS.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(EXPORT_COLUMNS.get(i).getName());
                    cell.setCellStyle(headerStyle);
                }

                int rowIndex = 1;
                for (TestCaseDto tc : testCases) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < EXPORT_COLUMNS.size(); i++) {
                        Cell cell = row.createCell(i);
                        String val = EXPORT_COLUMNS.get(i).getValueExtractor().apply(tc, project);
                        cell.setCellValue(val != null ? val : "");
                    }
                }

                for (int i = 0; i < EXPORT_COLUMNS.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                workbook.write(fos);
            }
        }

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(project, Notifier.class).infoWithActions(project,
                        "Export Complete", "Exported to: " + destFile.getName(),
                        NotificationAction.createSimple("Open file", () -> {
                            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(destFile.getAbsolutePath());
                            if (vf != null)
                                Services.getInstance(project, Tools.class).openWithAssociatedProgram(project, vf);
                        }))
        );
    }
}
