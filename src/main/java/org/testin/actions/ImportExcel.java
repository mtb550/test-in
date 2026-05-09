package org.testin.actions;

import com.codoid.products.fillo.Connection;
import com.codoid.products.fillo.Fillo;
import com.codoid.products.fillo.Recordset;
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
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.Group;
import org.testin.pojo.Priority;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ImportExcel extends DumbAwareAction {
    // todo, all patterns to be moved to Tools class.
    private final static Pattern SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9 _]");
    private final static Pattern STEP_MUSH_PATTERN = Pattern.compile(".*\\s\\d+[-.].*");
    private final static Pattern STEP_LINE_PATTERN = Pattern.compile("(\\s)(?=\\d+[-.])");
    private final static Pattern STEP_CLEAN_PATTERN = Pattern.compile("^\\d+[-.]\\s*");

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
            Notifier.error("Import Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof TestSetDirectoryDto ts)) {
            Notifier.error("Import Error", "Please select a valid Test Set Node.");
            return;
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(ts.getPath().toString());

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Notifier.error("Import Error", "The selected path in the Project Panel is invalid.");
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
                .withDescription("Please choose an .xls file");

        final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, Config.getProject(), null);

        if (selectedFile != null) {
            if (!"xls".equalsIgnoreCase(selectedFile.getExtension())) {
                ApplicationManager.getApplication().invokeLater(() -> Notifier.error("Invalid File Format",
                        "Only '.xls' files are allowed.\n\n" +
                                "You selected an '." + selectedFile.getExtension() + "' file.\n" +
                                "Please save your Excel file as 'Excel 97-2003 Workbook (*.xls)' and try again."));
                return;
            }
            processWithFillo(selectedFile.getPath(), targetDirectory, ts);
        }
    }

    // todo, to be removed and use Tools.getTestSourceRoot
    private void downloadSampleFile(AnActionEvent e) {
        if (e.getProject() == null) return;

        VirtualFile projectDir = LocalFileSystem.getInstance().findFileByPath(Objects.requireNonNull(e.getProject().getBasePath()));

        if (projectDir == null) {
            Notifier.error("Error", "Could not find the project directory.");
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            try (InputStream in = getClass().getResourceAsStream("/files/import_sample.xls")) {

                if (in == null) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.error("File Error", "Sample file not found inside the plugin resources!"));
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
                    Notifier.info("Sample Ready", "Sample file has been added to your project and opened in Excel.");
                });

            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Notifier.error("Download Error", "Failed to save sample file: " + ex.getMessage()));
            }
        });
    }

    private void processWithFillo(final String filePath, final VirtualFile targetDirectory, final TestSetDirectoryDto ts) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            Notifier.error("File Error", "Java cannot read this file!");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Importing test cases", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Connecting to Excel file...");

                System.setProperty("log4j2.disable.jmx", "true");
                Fillo fillo = new Fillo();

                ObjectMapper mapper = Config.getMapper();

                Connection connection = null;
                Recordset recordset = null;
                try {
                    // todo, expected result is not arranged if it is multi lines. to be fixed.
                    // todo, if import, we need generate code context menu, to generate all in one click.
                    // todo, filter by module in status bar

                    connection = fillo.getConnection(filePath); // todo, fetch sheet name dynamically (Sheet 1)
                    String actualSheetName = connection.getMetaData().getTableNames().getFirst();
                    String query = "SELECT * FROM \"" + actualSheetName + "\"";
                    System.out.println(query);
                    recordset = connection.executeQuery(query);

                    indicator.setText("Mapping column headers...");
                    Map<String, String> headerMap = buildCaseInsensitiveHeaderMap(recordset.getFieldNames(), IMPORT_COLUMNS);
                    Map<String, String> filesToWrite = new LinkedHashMap<>();

                    TestCaseDto previousTestCase = null;
                    int rowCount = 0;

                    indicator.setText("Parsing rows into JSON...");

                    while (recordset.next()) {
                        if (indicator.isCanceled()) break;

                        final TestCaseDto currentTestCase = new TestCaseDto().setId(UUID.randomUUID());

                        for (TestEditorAttributes attr : TestEditorAttributes.values()) {
                            if (attr.isImportValue()) {
                                String rawValue = getFieldSafe(recordset, attr.getName(), headerMap);
                                attr.getImportSetter().accept(ImportExcel.this, currentTestCase, rawValue);
                            }
                        }

                        if (previousTestCase == null) currentTestCase.setIsHead(true);
                        else {
                            currentTestCase.setIsHead(null);
                            previousTestCase.setNext(currentTestCase.getId());
                            filesToWrite.put(previousTestCase.getId() + ".json", mapper.writeValueAsString(previousTestCase));
                        }
                        currentTestCase.setNext(null);
                        previousTestCase = currentTestCase;

                        rowCount++;

                        if (rowCount % 50 == 0) {
                            indicator.setText2("Parsed " + rowCount + " test cases...");
                        }

                    }

                    if (previousTestCase != null && !indicator.isCanceled())
                        filesToWrite.put(previousTestCase.getId() + ".json", mapper.writeValueAsString(previousTestCase));

                    if (indicator.isCanceled()) {
                        Notifier.warn("Import Cancelled", "Import was cancelled by the user.");
                        return;
                    }

                    indicator.setText("Writing JSON files to disk...");
                    indicator.setText2("");

                    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
                        for (Map.Entry<String, String> entry : filesToWrite.entrySet()) {
                            try {
                                VirtualFile newJsonFile = targetDirectory.createChildData(this, entry.getKey());
                                newJsonFile.setBinaryContent(entry.getValue().getBytes(StandardCharsets.UTF_8));
                            } catch (IOException ex) {
                                System.err.println("Failed to write file: " + entry.getKey());
                            }
                        }
                        Notifier.info("Import Complete", "Successfully imported " + filesToWrite.size() + " test cases.");
                        targetDirectory.refresh(false, true);
                        Tools.getInstance().closeThenOpenTestEditor(targetDirectory, ts);
                    }));

                } catch (Exception ex) {
                    System.err.println("Import crashed: " + ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.error("Failed to import data: " +
                                    "\n(Tip: Ensure the file is completely closed in Microsoft Excel and try again.)\n"
                                    + ex.getMessage())
                    );
                } finally {
                    if (recordset != null) recordset.close();
                    if (connection != null) connection.close();
                }
            }
        });
    }

    private Map<String, String> buildCaseInsensitiveHeaderMap(final List<String> actualExcelColumns, final List<String> requestedColumns) {
        Map<String, String> map = new HashMap<>();
        for (String requested : requestedColumns) {
            actualExcelColumns.stream()
                    .filter(excelCol -> excelCol.equalsIgnoreCase(requested))
                    .findFirst()
                    .ifPresent(actualCasing -> map.put(requested.toLowerCase(), actualCasing));
        }
        return map;
    }

    private String getFieldSafe(final Recordset recordset, final String requestedFieldName, final Map<String, String> headerMap) {
        try {
            String exactMatchedColumn = headerMap.get(requestedFieldName.toLowerCase());

            if (exactMatchedColumn == null) {
                return "";
            }

            String value = recordset.getField(exactMatchedColumn);
            return (value != null && !value.isBlank()) ? value.trim() : "";

        } catch (Exception ex) {
            return "";
        }
    }

    // todo, move all below to Tools class
    public String sanitizeDescription(final String rawDesc) {
        if (rawDesc == null || rawDesc.isBlank()) return "EMPTY_DESCRIPTION";
        String cleaned = SANITIZE_PATTERN.matcher(rawDesc).replaceAll("").trim();
        return cleaned.isEmpty() ? "EMPTY_DESCRIPTION" : cleaned;
    }

    public List<String> parseStepsSafe(final String stepsRaw) {
        if (stepsRaw == null || stepsRaw.isBlank()) {
            return new ArrayList<>();
        }

        String text = stepsRaw;

        if (!text.contains("\n") && STEP_MUSH_PATTERN.matcher(text).matches()) {
            text = STEP_LINE_PATTERN.matcher(text).replaceAll("\n");
        }

        return Arrays.stream(text.split("\n"))
                .map(line -> STEP_CLEAN_PATTERN.matcher(line).replaceFirst("").trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    public Priority parsePrioritySafe(final String priorityStr) {
        if (priorityStr == null || priorityStr.isBlank()) {
            return Priority.LOW;
        }
        try {
            return Priority.valueOf(priorityStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Priority.LOW;
        }
    }

    public ZonedDateTime parseDateSafe(final String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
        try {
            return LocalDateTime.parse(dateStr, Config.EXCEL_DATE_FORMATTER).atZone(ZoneId.systemDefault());
        } catch (Exception e) {
            return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
    }

    public List<Group> parseGroupsSafe(final String rawGroups) {
        if (rawGroups == null || rawGroups.isBlank()) {
            return new ArrayList<>();
        }

        return Arrays.stream(rawGroups.split(","))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .map(String::toUpperCase)
                .map(groupName -> {
                    try {
                        return Group.valueOf(groupName);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}