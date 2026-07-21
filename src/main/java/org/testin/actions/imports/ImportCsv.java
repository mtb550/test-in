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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.FileTypes;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.ui.ImportPreviewDialog;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ImportCsv extends ImportBase {

    public ImportCsv(final @NotNull SimpleTree tree) {
        super(tree, "Import from CSV", "Import test cases from a CSV file", AllIcons.FileTypes.Csv);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ImportContext ctx = validateTreeSelection(e);
        if (ctx == null) return;

        String infoMsg = FileTypes.CSV.getInfoMessage(String.join(", ", IMPORT_COLUMNS));
        int userChoice = Messages.showDialog(
                ctx.project(),
                infoMsg,
                "CSV Import Requirements",
                new String[]{"Choose File...", "Cancel"},
                0,
                Messages.getInformationIcon()
        );

        if (userChoice == 0) {
            openFileChooserAndProcess(ctx.project(), ctx.targetDirectory(), ctx.dirDto(), ctx.parentNode());
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
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, "Invalid File Format",
                                "Only CSV files (.csv) are allowed."));
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
                                Services.getInstance(project, Notifier.class).warn(project, "No Data", "No valid test cases found in the CSV file."));
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
                    ImportPreviewDialog dialog = new ImportPreviewDialog(project, allSheetsData);
                    if (dialog.showAndGet()) {
                        Map<String, List<TestCaseDto>> selectedCasesBySheet = dialog.getSelectedTestCasesBySheet();
                        if (selectedCasesBySheet.isEmpty()) {
                            Services.getInstance(project, Notifier.class).softShow(project, "No Selection", "No test cases were selected for import.");
                            return;
                        }
                        executeImportWriteAction(project, targetDirectory, selectedDirDto, parentNode, dialog, selectedCasesBySheet, "ImportCsv");
                    } else {
                        Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled from preview dialog.");
                    }
                });
            }
        });
    }

    public List<TestCaseDto> parseFile(final @NotNull Project project, final File file) throws Exception {
        return parseCsvFile(file, project, null);
    }

    private List<TestCaseDto> parseCsvFile(final File file, final Project project, final ProgressIndicator indicator) throws Exception {
        List<TestCaseDto> result = new ArrayList<>();
        List<String[]> records = parseCsvRecords(file);

        if (records.isEmpty()) return result;

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
            if (indicator != null && indicator.isCanceled()) break;

            String[] values = records.get(r);

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
                if (attr.isImportable()) {
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

            if (totalParsed % 50 == 0 && indicator != null) {
                indicator.setText2("Parsed " + totalParsed + " test cases...");
            }
        }

        return result;
    }

    private List<String[]> parseCsvRecords(final File file) throws IOException {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (records.isEmpty() && line.charAt(0) == '\ufeff') {
                    line = line.substring(1);
                }

                String[] fields = parseCsvLine(line);
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
}
