package org.testin.actions.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.ui.ImportPreviewDialog;
import org.testin.util.Mapper;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.util.*;

public class ImportJson extends ImportBase {

    public ImportJson(final @NotNull SimpleTree tree) {
        super(tree, "Import from JSON", "Import test cases from a JSON file", AllIcons.FileTypes.Json);
    }

    public Map<String, List<TestCaseDto>> parseFile(final @NotNull Project project, final File file) {
        Map<String, List<TestCaseDto>> data = Services.getInstance(project, Mapper.class).readValue(file, new TypeReference<>() {
        });
        Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        if (data != null) {
            for (Map.Entry<String, List<TestCaseDto>> entry : data.entrySet()) {
                List<TestCaseDto> sanitized = new ArrayList<>();
                for (TestCaseDto tc : entry.getValue()) {
                    tc.setId(UUID.randomUUID());
                    tc.setIsHead(null);
                    tc.setNext(null);
                    sanitized.add(tc);
                }
                if (!sanitized.isEmpty()) {
                    result.put(entry.getKey(), sanitized);
                }
            }
        }
        return result;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ImportContext ctx = validateTreeSelection(e);
        if (ctx == null) return;

        openFileChooserAndProcess(ctx.project(), ctx.targetDirectory(), ctx.dirDto(), ctx.parentNode());
    }

    private void openFileChooserAndProcess(final @NotNull Project project, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select JSON File")
                .withDescription("Please choose an exported .json file");

        final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, project, null);

        if (selectedFile != null) {
            String extension = selectedFile.getExtension();
            if (extension == null || !extension.equalsIgnoreCase("json")) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, "Invalid File Format",
                                "Only JSON files (.json) are allowed."));
                return;
            }
            processWithJson(project, selectedFile.getPath(), targetDirectory, selectedDirDto, parentNode);
        }
    }

    private void processWithJson(final @NotNull Project project, final String filePath, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            Services.getInstance(project, Notifier.class).error(project, "File Error", "Java cannot read this file!");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Importing JSON test cases", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Parsing JSON file...");

                Map<String, List<TestCaseDto>> rawData = Services.getInstance(project, Mapper.class).readValue(file, new TypeReference<>() {
                });

                if (rawData == null) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).error(project,
                                    "Failed to parse JSON file. It may be corrupted or incorrectly formatted."));
                    return;
                }

                if (indicator.isCanceled()) return;

                Map<String, List<TestCaseDto>> sanitizedData = new LinkedHashMap<>();
                for (Map.Entry<String, List<TestCaseDto>> entry : rawData.entrySet()) {
                    List<TestCaseDto> sanitizedList = new ArrayList<>();
                    for (TestCaseDto tc : entry.getValue()) {
                        tc.setId(UUID.randomUUID());
                        tc.setIsHead(null);
                        tc.setNext(null);
                        sanitizedList.add(tc);
                    }
                    if (!sanitizedList.isEmpty()) {
                        sanitizedData.put(entry.getKey(), sanitizedList);
                    }
                }

                if (sanitizedData.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).warn(project, "No Data", "No valid test cases found in the JSON file."));
                    return;
                }

                indicator.setText("Waiting for user confirmation...");

                ApplicationManager.getApplication().invokeLater(() -> {
                    ImportPreviewDialog dialog = new
                            ImportPreviewDialog(project, sanitizedData);
                    dialog.setTitle("Preview & Select JSON Import");

                    if (dialog.showAndGet()) {
                        Map<String, List<TestCaseDto>> selectedCasesByGroup = dialog.getSelectedTestCasesBySheet();
                        if (selectedCasesByGroup.isEmpty()) {
                            Services.getInstance(project, Notifier.class).softShow(project, "No Selection", "No test cases were selected for import.");
                            return;
                        }
                        executeImportWriteAction(project, targetDirectory, selectedDirDto, parentNode, dialog, selectedCasesByGroup, "ImportJson");
                    } else {
                        Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled from preview dialog.");
                    }
                });
            }
        });
    }
}
