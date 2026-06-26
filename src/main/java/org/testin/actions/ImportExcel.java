package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.ui.ExcelPreviewDialog;
import org.testin.util.EditorUtil;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Services.getInstance(e.getProject(), Notifier.class).error(e.getProject(), "Import Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof DirectoryDto dirDto) ||
                !(dirDto instanceof TestSetDirectoryDto || dirDto instanceof TestSetPackageDirectoryDto || dirDto instanceof TestCasesMainDirectoryDto)) {
            Services.getInstance(e.getProject(), Notifier.class).error(e.getProject(), "Import Error", "Please select a valid Test Set, Test Set Package, or Test Cases Directory.");
            return;
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Services.getInstance(e.getProject(), Notifier.class).error(e.getProject(), "Import Error", "The selected path in the Project Panel is invalid.");
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
            openFileChooserAndProcess(e.getProject(), targetDirectory, dirDto, parentNode);
        } else if (userChoice == 1) {
            downloadSampleFile(e.getProject(), e);
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
                ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(project, Notifier.class).error(project, "Invalid File Format",
                        "Only Excel files (.xls, .xlsx) are allowed.\n\n" +
                                "You selected an '." + extension + "' file.\n" +
                                "Please select a valid Excel file and try again."));
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
            // todo, to be removed and use Tools.getTestSourceRoot
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

            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, "Download Error", "Failed to save sample file: " + ex.getMessage()));
            }
        });
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
                                    if (tc != null && tc.getNext() == null) {
                                        break;
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

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
                                if (attr.isImportValue()) {
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

                            if (totalParsed % 50 == 0) {
                                indicator.setText2("Parsed " + totalParsed + " test cases...");
                            }
                        }

                        if (!sheetPreviewList.isEmpty()) {
                            allSheetsData.put(sheetName, sheetPreviewList);
                        }
                    }

                } catch (Exception ex) {
                    Log.error("Import crashed: " + ex.getMessage());
                    Log.error("Exception: " + ex.getMessage());
                    Services.getInstance(project, Notifier.class).error(project, "Failed to import data: (Tip: Ensure the file is completely closed in Microsoft Excel and try again.)");
                    return;
                }

                if (indicator.isCanceled()) {
                    Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled by you.");
                    return;
                }

                if (allSheetsData.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).warn(project, "No Data", "No valid test cases found in the Excel file.")
                    );
                    return;
                }

                indicator.setText("Waiting for user confirmation...");
                indicator.setText2("");

                ApplicationManager.getApplication().invokeLater(() -> {
                    ExcelPreviewDialog dialog = new ExcelPreviewDialog(project, allSheetsData);

                    if (dialog.showAndGet()) {

                        Map<String, List<TestCaseDto>> selectedCasesBySheet = dialog.getSelectedTestCasesBySheet();

                        if (selectedCasesBySheet.isEmpty()) {
                            Services.getInstance(project, Notifier.class).softShow(project, "No Selection", "No test cases were selected for import.");
                            return;
                        }

                        ApplicationManager.getApplication().runWriteAction(() -> {
                            try {
                                if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                                    TestCaseDto tail = findExistingTail(project, targetDirectory);
                                    List<TestCaseDto> flatList = new ArrayList<>();
                                    selectedCasesBySheet.values().forEach(flatList::addAll);

                                    linkAndSaveTestCases(project, targetDirectory, flatList, tail);

                                    Services.getInstance(project, EditorUtil.class).closeThenOpenEditor(project, targetDirectory, ts);
                                    Services.getInstance(project, Notifier.class).info(project, "Import Complete", "Successfully imported " + flatList.size() + " test cases.");

                                } else {
                                    int totalImported = 0;
                                    for (Map.Entry<String, List<TestCaseDto>> entry : selectedCasesBySheet.entrySet()) {
                                        String rawSheetName = entry.getKey();
                                        List<TestCaseDto> sheetCases = entry.getValue();

                                        VirtualFile sheetDir = new CreateTestSet().inBackground(project, ImportExcel.this, targetDirectory, selectedDirDto, parentNode, tree, rawSheetName);

                                        TestCaseDto tail = findExistingTail(project, sheetDir);
                                        linkAndSaveTestCases(project, sheetDir, sheetCases, tail);
                                        totalImported += sheetCases.size();
                                    }
                                    Services.getInstance(project, Notifier.class).info(project, "Import Complete", "Successfully imported " + totalImported + " test cases into separate Test Sets.");
                                }

                                targetDirectory.refresh(false, true);

                            } catch (IOException ex) {
                                Log.error("Failed to write files: " + ex.getMessage());
                            }
                        });

                    } else {
                        Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled from preview dialog.");
                    }
                });
            }
        });
    }

    private void linkAndSaveTestCases(final @NotNull Project project, final VirtualFile dir, final List<TestCaseDto> testCases, final TestCaseDto existingTail) throws IOException {
        final Path dirPath = Path.of(dir.getPath());
        final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

        TestCaseDto previousNode = existingTail;

        for (TestCaseDto currentTestCase : testCases) {
            if (previousNode == null) {
                currentTestCase.setIsHead(true);
            } else {
                currentTestCase.setIsHead(null);
                previousNode.setNext(currentTestCase.getId());
            }
            currentTestCase.setNext(null);
            previousNode = currentTestCase;
        }

        if (existingTail != null) {
            indexer.putTestCase(dirPath, existingTail);
        }

        for (TestCaseDto tc : testCases) {
            indexer.putTestCase(dirPath, tc);
        }
    }

    private TestCaseDto findExistingTail(final @NotNull Project project, final VirtualFile directory) {
        if (directory == null) return null;
        VirtualFile[] children = directory.getChildren();
        if (children != null) {
            for (VirtualFile child : children) {
                if (!child.isDirectory() && child.getName().endsWith(".json")) {
                    try (InputStream is = child.getInputStream()) {
                        TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(is, TestCaseDto.class);
                        if (tc != null && tc.getNext() == null) {
                            return tc;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
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

        e.getPresentation().setEnabled(userObject instanceof TestSetDirectoryDto ||
                userObject instanceof TestSetPackageDirectoryDto ||
                userObject instanceof TestCasesMainDirectoryDto);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}