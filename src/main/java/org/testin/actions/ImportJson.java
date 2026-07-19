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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.nodeCreator.CreateTestSet;
import org.testin.pojo.dto.JsonExportDto;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class ImportJson extends DumbAwareAction {

    private final @NotNull SimpleTree tree;

    public ImportJson(final @NotNull SimpleTree tree) {
        super("Import from JSON", "Import test cases from a JSON file", AllIcons.FileTypes.Json);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final Project project = e.getProject();
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "Please select a directory in the Project Panel tree.");
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

        openFileChooserAndProcess(e.getProject(), targetDirectory, dirDto, parentNode);
    }

    private void openFileChooserAndProcess(final @NotNull Project project, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select JSON File")
                .withDescription("Please choose an exported .json file");

        final VirtualFile selectedFile = FileChooser.chooseFile(descriptor, project, null);

        if (selectedFile != null) {
            String extension = selectedFile.getExtension();

            if (extension == null || !extension.equalsIgnoreCase("json")) {
                ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(project, Notifier.class).error(project, "Invalid File Format",
                        "Only JSON files (.json) are allowed.\n\n" +
                                "You selected an '." + extension + "' file.\n" +
                                "Please select a valid JSON file and try again."));
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

                JsonExportDto exportDto = Services.getInstance(project, Mapper.class).readValue(file, JsonExportDto.class);

                if (exportDto == null) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).error(project, "Failed to parse JSON file. It may be corrupted or incorrectly formatted.")
                    );
                    return;
                }

                if (indicator.isCanceled()) return;

                Map<String, List<TestCaseDto>> sanitizedData = new LinkedHashMap<>();
                for (Map.Entry<String, List<TestCaseDto>> entry : exportDto.getData().entrySet()) {
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
                            Services.getInstance(project, Notifier.class).warn(project, "No Data", "No valid test cases found in the JSON file.")
                    );
                    return;
                }

                indicator.setText("Waiting for user confirmation...");

                ApplicationManager.getApplication().invokeLater(() -> {
                    ExcelPreviewDialog dialog = new ExcelPreviewDialog(project, sanitizedData);
                    dialog.setTitle("Preview & Select JSON Import");

                    if (dialog.showAndGet()) {
                        Map<String, List<TestCaseDto>> selectedCasesByGroup = dialog.getSelectedTestCasesBySheet();

                        if (selectedCasesByGroup.isEmpty()) {
                            Services.getInstance(project, Notifier.class).softShow(project, "No Selection", "No test cases were selected for import.");
                            return;
                        }

                        ApplicationManager.getApplication().runWriteAction(() -> {
                            try {
                                if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                                    TestCaseDto tail = findExistingTail(project, targetDirectory);
                                    List<TestCaseDto> flatList = new ArrayList<>();
                                    selectedCasesByGroup.values().forEach(flatList::addAll);

                                    linkAndSaveTestCases(project, targetDirectory, flatList, tail);

                                    if (dialog.getCodeGenerator().isSelected()) {
                                        Log.info("ImportJson: generating test methods for " + flatList.size() + " imported cases");
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
                                    for (Map.Entry<String, List<TestCaseDto>> entry : selectedCasesByGroup.entrySet()) {
                                        String rawGroupName = entry.getKey();
                                        List<TestCaseDto> groupCases = entry.getValue();

                                        VirtualFile groupDir = new CreateTestSet().inBackground(project, ImportJson.this, targetDirectory, selectedDirDto, parentNode, tree, rawGroupName);

                                        TestCaseDto tail = findExistingTail(project, groupDir);
                                        linkAndSaveTestCases(project, groupDir, groupCases, tail);

                                        if (dialog.getCodeGenerator().isSelected()) {
                                            String sheetName = groupDir.getName();
                                            TestSetDirectoryDto sheetDto = TestSetDirectoryDto.builder()
                                                    .name(sheetName)
                                                    .path(Path.of(groupDir.getPath()))
                                                    .path2(Services.getInstance(project, Tools.class).buildPath2(selectedDirDto.getPath2(), sheetName))
                                                    .parent(selectedDirDto)
                                                    .build();
                                            Log.info("ImportJson: generating test methods for group '" + sheetName + "' with " + groupCases.size() + " cases");
                                            CreateTestMethod syncInjector = new CreateTestMethod();
                                            for (TestCaseDto tc : groupCases) {
                                                tc.setParent(sheetDto);
                                                List<String> fqcn = Services.getInstance(project, Tools.class).buildFqcnMethod(tc);
                                                syncInjector.executeSync(project, tc, fqcn);
                                            }
                                        }

                                        totalImported += groupCases.size();
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