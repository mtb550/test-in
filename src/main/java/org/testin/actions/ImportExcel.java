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
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportExcel extends DumbAwareAction {
    private static final String EXCEL_INFO_MESSAGE =
            """
                    To ensure a successful import, your Excel file should contain the following column headers (case-insensitive):
                    
                     • Description
                     • Expected Result
                     • Steps
                     • Group
                     • Priority
                     • Reference
                     • Module
                     • Status
                     • Created By
                     • Created At
                     • Updated By
                     • Updated At
                    
                    Note: Missing columns will safely default to empty values.""";

    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9 _]");
    private static final Pattern STEP_MUSH_PATTERN = Pattern.compile(".*\\s\\d+[-.].*");
    private static final Pattern STEP_LINE_PATTERN = Pattern.compile("(\\s)(?=\\d+[-.])");
    private static final Pattern STEP_CLEAN_PATTERN = Pattern.compile("^\\d+[-.]\\s*");
    private final SimpleTree tree;

    public ImportExcel(SimpleTree tree) {
        super("From Excel", "Import test cases from excel", AllIcons.Providers.Microsoft);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Notifier.error("Import Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof TestSetDirectoryDto ts)) {
            Notifier.error("Import Error", "Please select a valid TS Directory.");
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

        int userChoice = Messages.showOkCancelDialog(
                e.getProject(),
                EXCEL_INFO_MESSAGE,
                "Excel Import Requirements",
                "Choose File...",
                "Cancel",
                Messages.getInformationIcon()
        );

        if (userChoice != Messages.OK) {
            return;
        }

        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select Spreadsheet File")
                .withDescription("Please choose an .xls or .xlsx file")
                .withFileFilter(virtualFile -> {
                    String ext = virtualFile.getExtension();
                    return "xlsx".equalsIgnoreCase(ext) || "xls".equalsIgnoreCase(ext);
                });

        final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, Config.getProject(), null);

        if (selectedFile != null) {
            processWithFillo(selectedFile.getPath(), targetDirectory);
        }
    }

    private void processWithFillo(String filePath, VirtualFile targetDirectory) {
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

                try {
                    final String description = TestEditorAttributes.DESCRIPTION.getName();
                    final String expectedResult = TestEditorAttributes.EXPECTED_RESULT.getName();
                    final String steps = TestEditorAttributes.STEPS.getName();
                    final String group = TestEditorAttributes.GROUP.getName();
                    final String priority = TestEditorAttributes.PRIORITY.getName();
                    final String reference = TestEditorAttributes.REFERENCE.getName();
                    final String module = TestEditorAttributes.MODULE.getName();
                    final String status = TestEditorAttributes.STATUS.getName();
                    final String createdBy = TestEditorAttributes.CREATE_BY.getName();
                    final String createdAt = TestEditorAttributes.CREATE_AT.getName();
                    final String updatedBy = TestEditorAttributes.UPDATE_BY.getName();
                    final String updatedAt = TestEditorAttributes.UPDATE_AT.getName();
                    // todo, implement order.
                    // todo, expected result is not arranged if it is multi lines. to be fixed.
                    // todo, if import, we need generate code context menu, to generate all in one click.
                    // todo, filter by module in status bar

                    final List<String> columns = Stream.of(description, expectedResult, steps, group, priority, reference, module, status, createdBy, createdAt, updatedBy, updatedAt)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());

                    Connection connection = fillo.getConnection(filePath); // todo, fetch sheet name dynamically (Sheet 1)
                    String actualSheetName = connection.getMetaData().getTableNames().getFirst();
                    String query = "SELECT * FROM \"" + actualSheetName + "\"";
                    System.out.println(query);
                    Recordset recordset = connection.executeQuery(query);

                    indicator.setText("Mapping column headers...");
                    Map<String, String> headerMap = buildCaseInsensitiveHeaderMap(recordset.getFieldNames(), columns);

                    Map<String, String> filesToWrite = new LinkedHashMap<>();
                    int rowCount = 0;

                    indicator.setText("Parsing rows into JSON...");

                    while (recordset.next()) {
                        if (indicator.isCanceled()) {
                            break;
                        }

                        final UUID generatedUuid = UUID.randomUUID();

                        TestCaseDto testCaseDto = new TestCaseDto()
                                .setId(generatedUuid)
                                .setDescription(sanitizeDescription(getFieldSafe(recordset, description, headerMap)))
                                .setExpectedResult(getFieldSafe(recordset, expectedResult, headerMap))
                                .setSteps(parseStepsSafe(getFieldSafe(recordset, steps, headerMap)))
                                .setGroup(parseGroupsSafe(getFieldSafe(recordset, group, headerMap)))
                                .setPriority(parsePrioritySafe(getFieldSafe(recordset, priority, headerMap)))
                                .setReference(getFieldSafe(recordset, reference, headerMap))
                                .setModule(getFieldSafe(recordset, module, headerMap))
                                .setStatus(getFieldSafe(recordset, status, headerMap))
                                .setCreatedBy(getFieldSafe(recordset, createdBy, headerMap))
                                .setCreatedAt(parseDateSafe(getFieldSafe(recordset, createdAt, headerMap)))
                                .setUpdatedBy(getFieldSafe(recordset, updatedBy, headerMap))
                                .setUpdatedAt(parseDateSafe(getFieldSafe(recordset, updatedAt, headerMap)));

                        filesToWrite.put(generatedUuid + ".json", mapper.writeValueAsString(testCaseDto));
                        rowCount++;

                        if (rowCount % 50 == 0) {
                            indicator.setText2("Parsed " + rowCount + " test cases...");
                        }
                    }

                    recordset.close();
                    connection.close();

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
                    }));

                } catch (Exception ex) {
                    System.err.println("Import crashed: " + ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.error("Failed to import data: " +
                                    "\n(Tip: Ensure the file is completely closed in Microsoft Excel and try again.)\n"
                                    + ex.getMessage())
                    );
                }
            }
        });
    }

    private Map<String, String> buildCaseInsensitiveHeaderMap(List<String> actualExcelColumns, List<String> requestedColumns) {
        Map<String, String> map = new HashMap<>();
        for (String requested : requestedColumns) {
            actualExcelColumns.stream()
                    .filter(excelCol -> excelCol.equalsIgnoreCase(requested))
                    .findFirst()
                    .ifPresent(actualCasing -> map.put(requested, actualCasing));
        }
        return map;
    }

    private String getFieldSafe(final Recordset recordset, final String requestedFieldName, Map<String, String> headerMap) {
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

    private String sanitizeDescription(String rawDesc) {
        if (rawDesc == null || rawDesc.isBlank()) return "EMPTY_DESCRIPTION";
        String cleaned = SANITIZE_PATTERN.matcher(rawDesc).replaceAll("").trim();
        return cleaned.isEmpty() ? "EMPTY_DESCRIPTION" : cleaned;
    }

    private List<String> parseStepsSafe(String stepsRaw) {
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

    private Priority parsePrioritySafe(String priorityStr) {
        if (priorityStr == null || priorityStr.isBlank()) {
            return Priority.LOW;
        }
        try {
            return Priority.valueOf(priorityStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Priority.LOW;
        }
    }

    private ZonedDateTime parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
        try {
            return LocalDateTime.parse(dateStr, Config.EXCEL_DATE_FORMATTER).atZone(ZoneId.systemDefault());
        } catch (Exception e) {
            return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
    }

    private List<Group> parseGroupsSafe(final String rawGroups) {
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