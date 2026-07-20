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
import org.jetbrains.annotations.NotNull;
import org.testin.actions.nodeCreator.CreateTestSet;
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
import org.testin.util.autoGenerator.CreateTestMethod;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ImportCsv extends DumbAwareAction {
    private final @NotNull SimpleTree tree;

    private final List<String> IMPORT_COLUMNS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportValue)
            .map(TestEditorAttributes::getName)
            .toList();

    private final String CSV_INFO_MESSAGE =
            String.format("""
                            To ensure a successful import, your CSV file should contain the following column headers (case-insensitive):
                            
                            %s
                            
                            Note: Missing columns will safely default to empty values.
                            The CSV should use comma as delimiter. Values containing commas or newlines must be quoted with double quotes.""",
                    String.join(", ", IMPORT_COLUMNS));

    public ImportCsv(final @NotNull SimpleTree tree) {
        super("Import from CSV", "Import test cases from a CSV file", AllIcons.FileTypes.Csv);
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
                CSV_INFO_MESSAGE,
                "CSV Import Requirements",
                new String[]{"Choose File...", "Cancel"},
                0,
                Messages.getInformationIcon()
        );

        if (userChoice == 0) {
            openFileChooserAndProcess(e.getProject(), targetDirectory, dirDto, parentNode);
        }
    }

    private void openFileChooserAndProcess(final @NotNull Project project, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select CSV File")
                .withExtensionFilter("CSV files", "csv")
                .withDescription("Please choose a .csv file");

        final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, project, null);

        if (selectedFile != null) {
            String extension = selectedFile.getExtension();

            if (extension == null || !extension.equalsIgnoreCase("csv")) {
                ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(project, Notifier.class).error(project, "Invalid File Format",
                        "Only CSV files (.csv) are allowed.\n\n" +
                                "You selected an '." + extension + "' file.\n" +
                                "Please select a valid CSV file and try again."));
                return;
            }
            processCsvFile(project, selectedFile.getPath(), targetDirectory, selectedDirDto, parentNode);
        }
    }

    private void processCsvFile(final @NotNull Project project, final String filePath, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            Services.getInstance(project, Notifier.class).error(project, "File Error", "Java cannot read this file!");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Importing test cases from CSV", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Reading CSV file...");

                Map<String, List<TestCaseDto>> allSheetsData = new LinkedHashMap<>();

                try {
                    indicator.setText("Parsing CSV rows...");

                    List<TestCaseDto> testCases = parseCsvFile(file, project, indicator);

                    if (indicator.isCanceled()) {
                        Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled by you.");
                        return;
                    }

                    if (testCases.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(project, Notifier.class).warn(project, "No Data", "No valid test cases found in the CSV file.")
                        );
                        return;
                    }

                    String fileName = file.getName().replaceAll("\\.csv$", "").replaceAll("[\\\\/*?\\[\\]]", "_");
                    allSheetsData.put(fileName, testCases);

                } catch (final Exception ex) {
                    Log.error("Import crashed: " + ex.getMessage());
                    Services.getInstance(project, Notifier.class).error(project, "Failed to import data: " + ex.getMessage());
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

                                    if (dialog.getCg().isSelected()) {
                                        Log.info("ImportCsv: generating test methods for " + flatList.size() + " imported cases");
                                        CreateTestMethod syncInjector = new CreateTestMethod();
                                        for (TestCaseDto tc : flatList) {
                                            tc.setParent(ts);
                                            List<String> fqcn = Services.getInstance(project, Tools.class).buildFqcnMethod(tc);
                                            syncInjector.executeSync(project, tc, fqcn);
                                        }
                                    }

                                    Services.getInstance(project, EditorUtil.class).closeThenOpenEditor(project, targetDirectory, ts);
                                    Services.getInstance(project, Notifier.class).info(project, "Import Complete", "Successfully imported " + flatList.size() + " test cases.");
                                } else {
                                    int totalImported = 0;
                                    for (Map.Entry<String, List<TestCaseDto>> entry : selectedCasesBySheet.entrySet()) {
                                        String rawSheetName = entry.getKey();
                                        List<TestCaseDto> sheetCases = entry.getValue();

                                        VirtualFile sheetDir = new CreateTestSet().inBackground(project, ImportCsv.this, targetDirectory, selectedDirDto, parentNode, tree, rawSheetName);

                                        TestCaseDto tail = findExistingTail(project, sheetDir);
                                        linkAndSaveTestCases(project, sheetDir, sheetCases, tail);

                                        if (dialog.getCg().isSelected()) {
                                            String sheetName = sheetDir.getName();
                                            TestSetDirectoryDto sheetDto = TestSetDirectoryDto.builder()
                                                    .name(sheetName)
                                                    .path(Path.of(sheetDir.getPath()))
                                                    .path2(Services.getInstance(project, Tools.class).buildPath2(selectedDirDto.getPath2(), sheetName))
                                                    .parent(selectedDirDto)
                                                    .build();
                                            Log.info("ImportCsv: generating test methods for sheet '" + sheetName + "' with " + sheetCases.size() + " cases");
                                            CreateTestMethod syncInjector = new CreateTestMethod();
                                            for (TestCaseDto tc : sheetCases) {
                                                tc.setParent(sheetDto);
                                                List<String> fqcn = Services.getInstance(project, Tools.class).buildFqcnMethod(tc);
                                                syncInjector.executeSync(project, tc, fqcn);
                                            }
                                        }

                                        totalImported += sheetCases.size();
                                    }
                                    Services.getInstance(project, Notifier.class).info(project, "Import Complete", "Successfully imported " + totalImported + " test cases into separate Test Sets.");
                                }

                                targetDirectory.refresh(false, true);

                            } catch (final IOException ex) {
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

    private List<TestCaseDto> parseCsvFile(final File file, final Project project, final ProgressIndicator indicator) throws Exception {
        List<TestCaseDto> result = new ArrayList<>();
        List<String[]> records = parseCsvRecords(file);

        if (records.isEmpty()) return result;

        // First record is the header
        String[] headers = records.getFirst();
        Map<String, Integer> headerIndexMap = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            String headerName = headers[i].trim();
            for (String reqCol : IMPORT_COLUMNS) {
                if (reqCol.equalsIgnoreCase(headerName)) {
                    headerIndexMap.put(reqCol.toLowerCase(), i);
                }
            }
        }

        int totalParsed = 0;

        for (int r = 1; r < records.size(); r++) {
            if (indicator.isCanceled()) break;

            String[] values = records.get(r);

            // Check if row is empty
            boolean isRowEmpty = true;
            for (String val : values) {
                if (val != null && !val.trim().isEmpty()) {
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

                    if (colIndex != null && colIndex < values.length) {
                        String val = values[colIndex];
                        rawValue = val != null ? val.trim() : "";
                    }
                    attr.getImportSetter().accept(project, currentTestCase, rawValue);
                }
            }

            result.add(currentTestCase);
            totalParsed++;

            if (totalParsed % 50 == 0) {
                indicator.setText2("Parsed " + totalParsed + " test cases...");
            }
        }

        return result;
    }

    /**
     * Parses a CSV file line-by-line. Each line is split into fields respecting
     * double-quoted fields (commas inside quotes, escaped quotes "" -> ").
     * Strips UTF-8 BOM from the first line if present.
     */
    private List<String[]> parseCsvRecords(final File file) throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Strip UTF-8 BOM from the first line
                if (records.isEmpty() && line.charAt(0) == '\ufeff') {
                    line = line.substring(1);
                }

                String[] fields = parseCsvLine(line);

                // Skip completely empty rows (all fields blank)
                boolean allEmpty = true;
                for (String f : fields) {
                    if (f != null && !f.isEmpty()) {
                        allEmpty = false;
                        break;
                    }
                }
                if (!allEmpty) {
                    records.add(fields);
                }
            }
        }

        return records;
    }

    /**
     * Parses a single CSV line into fields.
     * Handles commas inside quotes and escaped double quotes ("").
     */
    private String[] parseCsvLine(final String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes) {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    inQuotes = true;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
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
