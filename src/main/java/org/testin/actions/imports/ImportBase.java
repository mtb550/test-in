package org.testin.actions.imports;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.nodeCreator.CreateTestSet;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.ui.ImportPreviewDialog;
import org.testin.util.EditorUtil;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.autoGenerator.CreateTestMethod;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class ImportBase extends DumbAwareAction {
    final @NotNull SimpleTree tree;

    final List<String> IMPORT_COLUMNS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportable)
            .map(TestEditorAttributes::getName)
            .toList();

    public ImportBase(final @NotNull SimpleTree tree, final @NotNull String text, final @NotNull String description, final @NotNull Icon icon) {
        super(text, description, icon);
        this.tree = tree;
    }

    @Nullable
    protected ImportContext validateTreeSelection(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return null;
        final Project project = e.getProject();
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "Please select a directory in the Project Panel tree.");
            return null;
        }

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof DirectoryDto dirDto) ||
                !(dirDto instanceof TestSetDirectoryDto || dirDto instanceof TestSetPackageDirectoryDto || dirDto instanceof TestCasesMainDirectoryDto)) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "Please select a valid Test Set, Test Set Package, or Test Cases Directory.");
            return null;
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "The selected path in the Project Panel is invalid.");
            return null;
        }

        return new ImportContext(project, targetDirectory, dirDto, parentNode);
    }

    protected void executeImportWriteAction(
            final @NotNull Project project,
            final VirtualFile targetDirectory,
            final DirectoryDto selectedDirDto,
            final DefaultMutableTreeNode parentNode,
            final ImportPreviewDialog dialog,
            final Map<String, List<TestCaseDto>> selectedCasesBySheet,
            final String logPrefix) {

        final ImportBase self = this;

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                    TestCaseDto tail = self.findExistingTail(project, targetDirectory);
                    List<TestCaseDto> flatList = new ArrayList<>();
                    selectedCasesBySheet.values().forEach(flatList::addAll);

                    self.linkAndSaveTestCases(project, targetDirectory, flatList, tail);

                    if (dialog.getCg().isSelected()) {
                        Log.info(logPrefix + ": generating test methods for " + flatList.size() + " imported cases");
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

                        VirtualFile sheetDir = new CreateTestSet().inBackground(project, self, targetDirectory, selectedDirDto, parentNode, tree, rawSheetName);

                        TestCaseDto tail = self.findExistingTail(project, sheetDir);
                        self.linkAndSaveTestCases(project, sheetDir, sheetCases, tail);

                        if (dialog.getCg().isSelected()) {
                            String sheetName = sheetDir.getName();
                            TestSetDirectoryDto sheetDto = TestSetDirectoryDto.builder()
                                    .name(sheetName)
                                    .path(Path.of(sheetDir.getPath()))
                                    .path2(Services.getInstance(project, Tools.class).buildPath2(selectedDirDto.getPath2(), sheetName))
                                    .parent(selectedDirDto)
                                    .build();
                            Log.info(logPrefix + ": generating test methods for sheet '" + sheetName + "' with " + sheetCases.size() + " cases");
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
    }

    public void linkAndSaveTestCases(final @NotNull Project project, final VirtualFile dir, final List<TestCaseDto> testCases, final TestCaseDto existingTail) throws IOException {
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

    @Nullable
    public TestCaseDto findExistingTail(final @NotNull Project project, final VirtualFile directory) {
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

    protected record ImportContext(
            @NotNull Project project,
            @NotNull VirtualFile targetDirectory,
            @NotNull DirectoryDto dirDto,
            @NotNull DefaultMutableTreeNode parentNode
    ) {
    }
}
