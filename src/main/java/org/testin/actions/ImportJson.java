package org.testin.actions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
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

public class ImportJson extends DumbAwareAction {

    private final SimpleTree tree;

    public ImportJson(final SimpleTree tree) {
        super("From JSON", "Import test cases from a JSON file", AllIcons.FileTypes.Json);
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

        if (!(userObject instanceof DirectoryDto dirDto) ||
                !(dirDto instanceof TestSetDirectoryDto || dirDto instanceof TestSetPackageDirectoryDto || dirDto instanceof TestCasesMainDirectoryDto)) {
            Notifier.getInstance().error("Import Error", "Please select a valid Test Set, Test Set Package, or Test Cases Directory.");
            return;
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Notifier.getInstance().error("Import Error", "The selected path in the Project Panel is invalid.");
            return;
        }

        openFileChooserAndProcess(targetDirectory, dirDto, parentNode);
    }

    private void openFileChooserAndProcess(final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select JSON File")
                .withDescription("Please choose an exported .json file");

        final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, Config.getProject(), null);

        if (selectedFile != null) {
            String extension = selectedFile.getExtension();

            if (extension == null || !extension.equalsIgnoreCase("json")) {
                ApplicationManager.getApplication().invokeLater(() -> Notifier.getInstance().error("Invalid File Format",
                        "Only JSON files (.json) are allowed.\n\n" +
                                "You selected an '." + extension + "' file.\n" +
                                "Please select a valid JSON file and try again."));
                return;
            }
            processWithJson(selectedFile.getPath(), targetDirectory, selectedDirDto, parentNode);
        }
    }

    private void processWithJson(final String filePath, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            Notifier.getInstance().error("File Error", "Java cannot read this file!");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Importing JSON test cases", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Parsing JSON file...");

                ObjectMapper mapper = Config.getMapper();
                Map<String, List<TestCaseDto>> rawData;

                try {
                    rawData = mapper.readValue(file, new TypeReference<>() {
                    });
                } catch (Exception ex) {
                    System.err.println("JSON Import crashed: " + ex.getMessage());
                    ex.printStackTrace(System.err);

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Failed to parse JSON file:\n" + ex.getMessage())
                    );
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
                            Notifier.getInstance().warn("No Data", "No valid test cases found in the JSON file.")
                    );
                    return;
                }

                indicator.setText("Waiting for user confirmation...");

                ApplicationManager.getApplication().invokeLater(() -> {
                    ExcelPreviewDialog dialog = new ExcelPreviewDialog(Config.getProject(), sanitizedData);
                    dialog.setTitle("Preview & Select JSON Import");

                    if (dialog.showAndGet()) {
                        Map<String, List<TestCaseDto>> selectedCasesByGroup = dialog.getSelectedTestCasesBySheet();

                        if (selectedCasesByGroup.isEmpty()) {
                            Notifier.getInstance().softShow("No Selection", "No test cases were selected for import.");
                            return;
                        }

                        ApplicationManager.getApplication().runWriteAction(() -> {
                            try {
                                if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                                    TestCaseDto tail = findExistingTail(targetDirectory, mapper);
                                    List<TestCaseDto> flatList = new ArrayList<>();
                                    selectedCasesByGroup.values().forEach(flatList::addAll);

                                    linkAndSaveTestCases(targetDirectory, flatList, tail, mapper, ImportJson.this);

                                    Tools.getInstance().closeThenOpenTestEditor(targetDirectory, ts);
                                    Notifier.getInstance().info("Import Complete", "Successfully imported " + flatList.size() + " test cases.");

                                } else {
                                    int totalImported = 0;
                                    for (Map.Entry<String, List<TestCaseDto>> entry : selectedCasesByGroup.entrySet()) {
                                        String rawGroupName = entry.getKey();
                                        List<TestCaseDto> groupCases = entry.getValue();

                                        VirtualFile groupDir = new CreateTestSet().inBackground(ImportJson.this, targetDirectory, selectedDirDto, parentNode, tree, rawGroupName);

                                        TestCaseDto tail = findExistingTail(groupDir, mapper);
                                        linkAndSaveTestCases(groupDir, groupCases, tail, mapper, ImportJson.this);
                                        totalImported += groupCases.size();
                                    }
                                    Notifier.getInstance().info("Import Complete", "Successfully imported " + totalImported + " test cases into separate Test Sets.");
                                }

                                targetDirectory.refresh(false, true);

                            } catch (IOException ex) {
                                System.err.println("Failed to write files: " + ex.getMessage());
                            }
                        });

                    } else {
                        Notifier.getInstance().softShow("Import Cancelled", "Import was cancelled from preview dialog.");
                    }
                });
            }
        });
    }

    private void linkAndSaveTestCases(final VirtualFile dir, final List<TestCaseDto> testCases, final TestCaseDto existingTail, final ObjectMapper mapper, final Object requestor) throws IOException {
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
            VirtualFile tailFile = dir.findChild(existingTail.getId() + ".json");
            if (tailFile != null) {
                tailFile.setBinaryContent(mapper.writeValueAsString(existingTail).getBytes(StandardCharsets.UTF_8));
            }
        }

        for (TestCaseDto tc : testCases) {
            VirtualFile newJsonFile = dir.createChildData(requestor, tc.getId() + ".json");
            newJsonFile.setBinaryContent(mapper.writeValueAsString(tc).getBytes(StandardCharsets.UTF_8));
        }
    }

    private TestCaseDto findExistingTail(final VirtualFile directory, final ObjectMapper mapper) {
        if (directory == null) return null;
        VirtualFile[] children = directory.getChildren();
        if (children != null) {
            for (VirtualFile child : children) {
                if (!child.isDirectory() && child.getName().endsWith(".json")) {
                    try (InputStream is = child.getInputStream()) {
                        TestCaseDto tc = mapper.readValue(is, TestCaseDto.class);
                        if (tc.getNext() == null) {
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
