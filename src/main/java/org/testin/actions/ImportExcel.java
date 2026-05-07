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
import com.intellij.openapi.project.DumbAwareAction;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportExcel extends DumbAwareAction {

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

        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                // todo, need to show a pop up or notice or info to inform user which columns are expected.
                // todo, try to add it as a separate text dialog in setting pop up.
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
        System.out.println("--- STARTING FILLO IMPORT ---");

        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            System.err.println("CRITICAL: Java cannot see or read this file!");
            return;
        }

        System.setProperty("log4j2.disable.jmx", "true");
        Fillo fillo = new Fillo();

        // todo, use Config.mapper if same logic
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

            final List<String> columns = Stream.of(description, expectedResult, steps, group, priority, reference, module, status, createdBy, createdAt, updatedBy, updatedAt)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            Connection connection = fillo.getConnection(filePath);
            String query = String.format("SELECT %s FROM Sheet1", String.join(", ", columns));

            Recordset recordset = connection.executeQuery(query);

            Map<String, String> filesToWrite = new LinkedHashMap<>();

            while (recordset.next()) {
                final UUID generatedUuid = UUID.randomUUID();

                TestCaseDto testCaseDto = new TestCaseDto()
                        .setId(generatedUuid)
                        .setDescription(sanitizeDescription(getFieldSafe(recordset, description)))
                        .setExpectedResult(getFieldSafe(recordset, expectedResult))
                        .setSteps(parseStepsSafe(getFieldSafe(recordset, steps)))
                        .setGroup(parseGroupsSafe(getFieldSafe(recordset, group)))
                        .setPriority(parsePrioritySafe(getFieldSafe(recordset, priority)))
                        .setReference(getFieldSafe(recordset, reference))
                        .setModule(getFieldSafe(recordset, module))
                        .setStatus(getFieldSafe(recordset, status))
                        .setCreatedBy(getFieldSafe(recordset, createdBy))
                        .setCreatedAt(parseDateSafe(getFieldSafe(recordset, createdAt)))
                        .setUpdatedBy(getFieldSafe(recordset, updatedBy))
                        .setUpdatedAt(parseDateSafe(getFieldSafe(recordset, updatedAt)));

                // todo, use Config.mapper if same logic
                String jsonContent = mapper.writeValueAsString(testCaseDto);
                filesToWrite.put(generatedUuid + ".json", jsonContent);

                System.out.println("Queued JSON for: " + testCaseDto.getDescription());
            }

            recordset.close();
            connection.close();

            ApplicationManager.getApplication().runWriteAction(() -> {
                for (Map.Entry<String, String> entry : filesToWrite.entrySet()) {
                    try {
                        VirtualFile newJsonFile = targetDirectory.createChildData(this, entry.getKey());
                        newJsonFile.setBinaryContent(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        System.err.println("Failed to write file: " + entry.getKey());
                        ex.printStackTrace(System.err);
                    }
                }
            });

            Notifier.info("Import Complete", "Successfully imported " + filesToWrite.size() + " test cases.");

        } catch (Exception ex) {
            System.err.println("Import crashed: " + ex.getMessage());
            Notifier.error("Import Error", "Failed to import data: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            System.out.println("--- IMPORT FINISHED ---");
        }
    }

    private String getFieldSafe(final Recordset recordset, final String requestedFieldName) {
        try {
            List<String> actualExcelColumns = recordset.getFieldNames();

            String exactMatchedColumn = actualExcelColumns.stream()
                    .filter(excelCol -> excelCol.equalsIgnoreCase(requestedFieldName))
                    .findFirst()
                    .orElse(null);

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
        String cleaned = rawDesc.replaceAll("[^a-zA-Z0-9 _]", "").trim();
        return cleaned.isEmpty() ? "EMPTY_DESCRIPTION" : cleaned;
    }

    private List<String> parseStepsSafe(String stepsRaw) {
        if (stepsRaw == null || stepsRaw.isBlank()) {
            return new ArrayList<>();
        }

        String text = stepsRaw;

        if (!text.contains("\n") && text.matches(".*\\s\\d+[-.].*")) {
            text = text.replaceAll("(\\s)(?=\\d+[-.])", "\n");
        }

        return Arrays.stream(text.split("\n"))
                .map(line -> line.replaceFirst("^\\d+[-.]\\s*", "").trim())
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
            System.err.println("Warning: Unknown priority '" + priorityStr + "', defaulting to LOW.");
            return Priority.LOW;
        }
    }

    private ZonedDateTime parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return ZonedDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        }
        try {
            return LocalDateTime.parse(dateStr, Config.getDateFormatter()).atZone(ZoneId.systemDefault());
        } catch (Exception e) {
            return ZonedDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
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
                        System.err.println("[WARNING] Unknown Group found in Excel: " + groupName);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}