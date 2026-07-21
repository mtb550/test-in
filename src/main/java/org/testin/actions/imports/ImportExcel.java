package org.testin.actions.imports;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.FileTypes;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.ui.ImportPreviewDialog;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class ImportExcel extends ImportBase {

    public ImportExcel(final @NotNull SimpleTree tree) {
        super(tree, "Import from Excel", "Import test cases from an excel file", AllIcons.FileTypes.MicrosoftWindows);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ImportContext ctx = validateTreeSelection(e);
        if (ctx == null) return;

        String infoMsg = FileTypes.XLSX.getInfoMessage(String.join("\n ", IMPORT_COLUMNS));
        int userChoice = Messages.showDialog(
                ctx.project(),
                infoMsg,
                "Excel Import Requirements",
                new String[]{"Choose File...", "Download Sample", "Cancel"},
                0,
                Messages.getInformationIcon()
        );

        if (userChoice == 0) {
            openFileChooserAndProcess(ctx.project(), ctx.targetDirectory(), ctx.dirDto(), ctx.parentNode());
        } else if (userChoice == 1) {
            downloadSampleFile(ctx.project(), e);
        }
    }

    private void openFileChooserAndProcess(final @NotNull Project project, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select Spreadsheet File")
                .withExtensionFilter("Excel workbooks", "xls", "xlsx")
                .withDescription("Please choose an .xls or .xlsx file");

        final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, project, null);

        if (selectedFile != null) {
            String extension = selectedFile.getExtension();
            if (extension == null || (!extension.equalsIgnoreCase("xls") && !extension.equalsIgnoreCase("xlsx"))) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, "Invalid File Format",
                                "Only Excel files (.xls, .xlsx) are allowed."));
                return;
            }
            processWithPoi(project, selectedFile.getPath(), targetDirectory, selectedDirDto, parentNode);
        }
    }

    private void downloadSampleFile(final @NotNull Project project, final AnActionEvent e) {
        if (e.getProject() == null) return;

        VirtualFile projectDir = LocalFileSystem.getInstance().findFileByPath(Objects.requireNonNull(e.getProject().getBasePath()));
        if (projectDir == null) {
            Services.getInstance(project, Notifier.class).error(project, "Error", "Could not find the project directory.");
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            try (InputStream in = getClass().getResourceAsStream("/files/import_sample.xls")) {
                if (in == null) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).error(project, "File Error", "Sample file not found inside the plugin resources!"));
                    return;
                }

                byte[] bytes = in.readAllBytes();
                VirtualFile newFile = projectDir.findChild("import_sample.xls");
                if (newFile == null) {
                    newFile = projectDir.createChildData(this, "import_sample.xls");
                }
                newFile.setBinaryContent(bytes);

                final VirtualFile fileToOpen = newFile;
                ApplicationManager.getApplication().invokeLater(() -> {
                    Services.getInstance(project, Tools.class).openWithAssociatedProgram(project, fileToOpen);
                    Services.getInstance(project, Notifier.class).info(project, "Sample Ready", "Sample file has been added to your project and opened in Excel.");
                });

            } catch (final Exception ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, "Download Error", "Failed to save sample file: " + ex.getMessage()));
            }
        });
    }

    public Map<String, List<TestCaseDto>> parseFile(final @NotNull Project project, final File file) throws Exception {
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        try (InputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            parseWorkbook(workbook, project, null, result);
        }
        return result;
    }

    private void parseWorkbook(final Workbook workbook, final @NotNull Project project, final @Nullable ProgressIndicator indicator, final Map<String, List<TestCaseDto>> result) {
        int totalParsed = 0;

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) continue;

            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) continue;

            DataFormatter dataFormatter = new DataFormatter();
            Map<String, Integer> headerIndexMap = new HashMap<>();

            for (Cell cell : headerRow) {
                String headerName = dataFormatter.formatCellValue(cell).trim();
                for (String reqCol : IMPORT_COLUMNS) {
                    if (reqCol.equalsIgnoreCase(headerName)) {
                        headerIndexMap.put(reqCol.toLowerCase(), cell.getColumnIndex());
                    }
                }
            }

            List<TestCaseDto> sheetPreviewList = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                if (indicator != null && indicator.isCanceled()) break;

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

                sheetPreviewList.add(currentTestCase);
                totalParsed++;

                if (totalParsed % 50 == 0 && indicator != null) {
                    indicator.setText2("Parsed " + totalParsed + " test cases...");
                }
            }

            if (!sheetPreviewList.isEmpty()) {
                result.put(sheetName, sheetPreviewList);
            }
        }
    }

    private void processWithPoi(final @NotNull Project project, final String filePath, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            Services.getInstance(project, Notifier.class).error(project, "File Error", "Java cannot read this file!");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Importing test cases", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Connecting to Excel file...");

                Map<String, List<TestCaseDto>> allSheetsData = new LinkedHashMap<>();

                try (InputStream fis = new FileInputStream(file);
                     Workbook workbook = WorkbookFactory.create(fis)) {

                    indicator.setText("Checking for existing test cases...");
                    VirtualFile[] existingChildren = targetDirectory.getChildren();
                    if (existingChildren != null) {
                        for (VirtualFile child : existingChildren) {
                            if (!child.isDirectory() && child.getName().endsWith(".json")) {
                                try (InputStream is = child.getInputStream()) {
                                    TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(is, TestCaseDto.class);
                                    if (tc != null && tc.getNext() == null) break;
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                    indicator.setText("Parsing rows into JSON...");
                    parseWorkbook(workbook, project, indicator, allSheetsData);

                } catch (final Exception ex) {
                    Log.error("Import crashed: " + ex.getMessage());
                    Services.getInstance(project, Notifier.class).error(project,
                            "Failed to import data: (Tip: Ensure the file is completely closed in Microsoft Excel and try again.)");
                    return;
                }

                if (indicator.isCanceled()) {
                    Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled by you.");
                    return;
                }

                if (allSheetsData.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).warn(project, "No Data", "No valid test cases found in the Excel file."));
                    return;
                }

                indicator.setText("Waiting for user confirmation...");
                indicator.setText2("");

                ApplicationManager.getApplication().invokeLater(() -> {
                    ImportPreviewDialog dialog = new ImportPreviewDialog(project, allSheetsData);
                    if (dialog.showAndGet()) {
                        Map<String, List<TestCaseDto>> selectedCasesBySheet = dialog.getSelectedTestCasesBySheet();
                        if (selectedCasesBySheet.isEmpty()) {
                            Services.getInstance(project, Notifier.class).softShow(project, "No Selection", "No test cases were selected for import.");
                            return;
                        }
                        executeImportWriteAction(project, targetDirectory, selectedDirDto, parentNode, dialog, selectedCasesBySheet, "ImportExcel");
                    } else {
                        Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled from preview dialog.");
                    }
                });
            }
        });
    }
}
