package testGit.actions;

import com.codoid.products.exception.FilloException;
import com.codoid.products.fillo.Connection;
import com.codoid.products.fillo.Fillo;
import com.codoid.products.fillo.Recordset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
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
import testGit.pojo.Config;
import testGit.pojo.Priority;
import testGit.pojo.TestSet;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.projectPanel.ProjectPanel;
import testGit.util.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

public class ImportExcel extends DumbAwareAction {

    private final SimpleTree testCasesTree;

    public ImportExcel(ProjectPanel projectPanel) {
        super("From Excel", "Import test cases from excel", AllIcons.Providers.Microsoft);
        this.testCasesTree = projectPanel.getProjectTree().getMainTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TreePath path = testCasesTree.getSelectionPath();
        if (path == null) {
            Notifier.error("Import Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof TestSet ts)) {
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

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select Spreadsheet File")
                .withDescription("Please choose an .xls or .xlsx file")
                .withFileFilter(virtualFile -> {
                    String ext = virtualFile.getExtension();
                    return "xlsx".equalsIgnoreCase(ext) || "xls".equalsIgnoreCase(ext);
                });

        VirtualFile selectedFile = FileChooser.chooseFile(descriptor, Config.getProject(), null);

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

        Gson gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (localDateTime, type, context) ->
                        new JsonPrimitive(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .create();

        try {
            Connection connection = fillo.getConnection(filePath);
            String query = "SELECT title, expected, steps, priority, createBy, createAt FROM Sheet1";
            Recordset recordset = connection.executeQuery(query);

            int importedCount = 0;

            while (recordset.next()) {
                String title = recordset.getField("title");

                if (title == null || title.isBlank()) {
                    break;
                }

                TestCaseJsonMapper testCaseJsonMapper = new TestCaseJsonMapper();
                String generatedUuid = UUID.randomUUID().toString();

                testCaseJsonMapper.setId(generatedUuid);
                testCaseJsonMapper.setTitle(title);
                testCaseJsonMapper.setExpected(getFieldSafe(recordset, "expected"));
                testCaseJsonMapper.setSteps(getFieldSafe(recordset, "steps"));

                testCaseJsonMapper.setPriority(parsePrioritySafe(getFieldSafe(recordset, "priority")));

                testCaseJsonMapper.setGroups(new ArrayList<>());
                testCaseJsonMapper.setCreateBy(getFieldSafe(recordset, "createBy"));

                testCaseJsonMapper.setCreateAt(parseDateSafe(getFieldSafe(recordset, "createAt")));

                String jsonContent = gson.toJson(testCaseJsonMapper);
                String fileName = generatedUuid + ".json";

                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        VirtualFile newJsonFile = targetDirectory.createChildData(this, fileName);
                        newJsonFile.setBinaryContent(jsonContent.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        System.err.println("Failed to write file: " + fileName);
                        ex.printStackTrace(System.err);
                    }
                });

                System.out.println("Generated JSON for: " + title + " -> " + fileName);
                importedCount++;
            }

            recordset.close();
            connection.close();

            Notifier.info("Import Complete", "Successfully generated " + importedCount + " JSON files.");

        } catch (FilloException ex) {
            System.err.println("Fillo specific crash: " + ex.getMessage());
            Notifier.error("Import Error", "Fillo failed: " + ex.getMessage());
        } finally {
            System.out.println("--- IMPORT FINISHED ---");
        }
    }

    private String getFieldSafe(Recordset recordset, String fieldName) {
        try {
            String value = recordset.getField(fieldName);
            return (value != null && !value.isBlank()) ? value : null;
        } catch (Exception ex) {
            return null;
        }
    }

    // Helper to map Excel string to Priority Enum
    private Priority parsePrioritySafe(String priorityStr) {
        if (priorityStr == null || priorityStr.isBlank()) {
            return null;
        }
        try {
            return Priority.valueOf(priorityStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Unknown priority '" + priorityStr + "', leaving null.");
            return null;
        }
    }

    private LocalDateTime parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            System.err.println("Warning: Could not parse date '" + dateStr + "', leaving null.");
            return null;
        }
    }
}