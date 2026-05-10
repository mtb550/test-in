package org.testin.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.ui.ExcelPreviewDialog;
import org.testin.util.Tools;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ImportExcel extends DumbAwareAction {

    private final List<String> IMPORT_COLUMNS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportValue)
            .map(TestEditorAttributes::getName)
            .toList();

    private final String EXCEL_INFO_MESSAGE =
            String.format("""
                            To ensure a successful import, your Excel file should contain the following column headers (case-insensitive):
                            
                            %s
                            
                            Note: Missing columns will safely default to empty values.
                            You can also download a ready-to-use sample file using the button below.""",
                    String.join("\n ", IMPORT_COLUMNS));

    private final SimpleTree tree;

    public ImportExcel(final SimpleTree tree) {
        super("From Excel", "Import test cases from excel", AllIcons.Providers.Microsoft);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Notifier.getInstance().error("Import Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof TestSetDirectoryDto ts)) {
            Notifier.getInstance().error("Import Error", "Please select a valid Test Set Node.");
            return;
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(ts.getPath().toString());

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Notifier.getInstance().error("Import Error", "The selected path in the Project Panel is invalid.");
            return;
        }

        int userChoice = Messages.showDialog(
                e.getProject(),
                EXCEL_INFO_MESSAGE,
                "Excel Import Requirements",
                new String[]{"Choose File...", "Download Sample", "Cancel"},
                0,
                Messages.getInformationIcon()
        );

        if (userChoice == 0) {
            openFileChooserAndProcess(targetDirectory, ts);
        } else if (userChoice == 1) {
            downloadSampleFile(e);
        }
    }

    private void openFileChooserAndProcess(final VirtualFile targetDirectory, final TestSetDirectoryDto ts) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select Spreadsheet File")
                .withDescription("Please choose an .xls or .xlsx file");

        final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, Config.getProject(), null);

        if (selectedFile != null) {
            String extension = selectedFile.getExtension();

            if (extension == null || (!extension.equalsIgnoreCase("xls") && !extension.equalsIgnoreCase("xlsx"))) {
                ApplicationManager.getApplication().invokeLater(() -> Notifier.getInstance().error("Invalid File Format",
                        "Only Excel files (.xls, .xlsx) are allowed.\n\n" +
                                "You selected an '." + extension + "' file.\n" +
                                "Please select a valid Excel file and try again."));
                return;
            }
            processWithPoi(selectedFile.getPath(), targetDirectory, ts);
        }
    }

    private void downloadSampleFile(AnActionEvent e) {
        if (e.getProject() == null) return;

        VirtualFile projectDir = LocalFileSystem.getInstance().findFileByPath(Objects.requireNonNull(e.getProject().getBasePath()));

        if (projectDir == null) {
            Notifier.getInstance().error("Error", "Could not find the project directory.");
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            // todo, to be removed and use Tools.getTestSourceRoot
            try (InputStream in = getClass().getResourceAsStream("/files/import_sample.xls")) {

                if (in == null) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("File Error", "Sample file not found inside the plugin resources!"));
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
                    Tools.getInstance().openWithAssociatedProgram(fileToOpen);
                    Notifier.getInstance().info("Sample Ready", "Sample file has been added to your project and opened in Excel.");
                });

            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Notifier.getInstance().error("Download Error", "Failed to save sample file: " + ex.getMessage()));
            }
        });
    }

    private void processWithPoi(final String filePath, final VirtualFile targetDirectory, final TestSetDirectoryDto ts) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            Notifier.getInstance().error("File Error", "Java cannot read this file!");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Importing test cases", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Connecting to Excel file...");

                ObjectMapper mapper = Config.getMapper();

                try (Workbook workbook = WorkbookFactory.create(file)) {
                    // todo, expected result is not arranged if it is multi lines. to be fixed.
                    // todo, if import, we need generate code context menu, to generate all in one click.
                    // todo, filter by module in status bar
                    // todo, fetch sheet name dynamically (Sheet 1) or sheet(0), get all sheets in JBTable tabs

                    indicator.setText("Checking for existing test cases...");
                    TestCaseDto existingTail = null;

                    VirtualFile[] existingChildren = targetDirectory.getChildren();
                    if (existingChildren != null) {
                        for (VirtualFile child : existingChildren) {
                            if (!child.isDirectory() && child.getName().endsWith(".json")) {
                                try (InputStream is = child.getInputStream()) {
                                    TestCaseDto tc = mapper.readValue(is, TestCaseDto.class);
                                    if (tc.getNext() == null) {
                                        existingTail = tc;
                                        break;
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                    Map<String, List<TestCaseDto>> allSheetsData = new LinkedHashMap<>();
                    int totalParsed = 0;

                    indicator.setText("Parsing rows into JSON...");

                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

                        if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i))
                            continue;

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
                            if (indicator.isCanceled()) break;

                            Row row = sheet.getRow(r);
                            if (row == null) continue;

                            boolean isRowEmpty = true;
                            for (int c = 0; c < row.getLastCellNum(); c++) {
                                if (!dataFormatter.formatCellValue(row.getCell(c)).trim().isEmpty()) {
                                    isRowEmpty = false;
                                    break;
                                }
                            }
                            if (isRowEmpty) continue;

                            final TestCaseDto currentTestCase = new TestCaseDto().setId(UUID.randomUUID());
                            currentTestCase.setNext(null);
                            currentTestCase.setIsHead(null);

                            for (TestEditorAttributes attr : TestEditorAttributes.values()) {
                                if (attr.isImportValue()) {
                                    Integer colIndex = headerIndexMap.get(attr.getName().toLowerCase());
                                    String rawValue = "";

                                    if (colIndex != null) {
                                        Cell dataCell = row.getCell(colIndex);
                                        rawValue = dataFormatter.formatCellValue(dataCell).trim();
                                    }
                                    attr.getImportSetter().accept(currentTestCase, rawValue);
                                }
                            }

                            sheetPreviewList.add(currentTestCase);
                            totalParsed++;

                            if (totalParsed % 50 == 0) {
                                indicator.setText2("Parsed " + totalParsed + " test cases...");
                            }
                        }

                        if (!sheetPreviewList.isEmpty()) {
                            allSheetsData.put(sheetName, sheetPreviewList);
                        }
                    }

                    if (indicator.isCanceled()) {
                        Notifier.getInstance().softShow("Import Cancelled", "Import was cancelled by you.");
                        return;
                    }

                    if (allSheetsData.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Notifier.getInstance().warn("No Data", "No valid test cases found in the Excel file.")
                        );
                        return;
                    }

                    final TestCaseDto finalExistingTail = existingTail;

                    indicator.setText("Waiting for user confirmation...");
                    indicator.setText2("");

                    ApplicationManager.getApplication().invokeLater(() -> {
                        ExcelPreviewDialog dialog = new ExcelPreviewDialog(Config.getProject(), allSheetsData);

                        if (dialog.showAndGet()) {

                            List<TestCaseDto> selectedCasesToImport = dialog.getSelectedTestCases();

                            if (selectedCasesToImport.isEmpty()) {
                                Notifier.getInstance().softShow("No Selection", "No test cases were selected for import.");
                                return;
                            }

                            ApplicationManager.getApplication().runWriteAction(() -> {
                                try {
                                    TestCaseDto previousNode = finalExistingTail;

                                    for (TestCaseDto currentTestCase : selectedCasesToImport) {
                                        if (previousNode == null) {
                                            currentTestCase.setIsHead(true);
                                        } else {
                                            currentTestCase.setIsHead(null);
                                            previousNode.setNext(currentTestCase.getId());
                                        }
                                        currentTestCase.setNext(null);
                                        previousNode = currentTestCase;
                                    }

                                    if (finalExistingTail != null) {
                                        VirtualFile tailFile = targetDirectory.findChild(finalExistingTail.getId() + ".json");
                                        if (tailFile != null) {
                                            String tailJsonContent = mapper.writeValueAsString(finalExistingTail);
                                            tailFile.setBinaryContent(tailJsonContent.getBytes(StandardCharsets.UTF_8));
                                        }
                                    }

                                    for (TestCaseDto tc : selectedCasesToImport) {
                                        String jsonContent = mapper.writeValueAsString(tc);
                                        VirtualFile newJsonFile = targetDirectory.createChildData(this, tc.getId() + ".json");
                                        newJsonFile.setBinaryContent(jsonContent.getBytes(StandardCharsets.UTF_8));
                                    }

                                    Notifier.getInstance().info("Import Complete", "Successfully imported " + selectedCasesToImport.size() + " test cases.");
                                    targetDirectory.refresh(false, true);
                                    Tools.getInstance().closeThenOpenTestEditor(targetDirectory, ts);

                                } catch (IOException ex) {
                                    System.err.println("Failed to write files: " + ex.getMessage());
                                }
                            });

                        } else {
                            Notifier.getInstance().softShow("Import Cancelled", "Import was cancelled from preview dialog.");
                        }
                    });

                } catch (Exception ex) {
                    System.err.println("Import crashed: " + ex.getMessage());
                    ex.printStackTrace(System.err);

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Failed to import data: " +
                                    "\n(Tip: Ensure the file is completely closed in Microsoft Excel and try again.)\n"
                                    + ex.getMessage())
                    );
                }
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        final int selectionCount = tree.getSelectionCount();

        if (selectionCount != 1 || path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = selectedNode.getUserObject();

        e.getPresentation().setEnabled(userObject instanceof TestSetDirectoryDto);
    }
}