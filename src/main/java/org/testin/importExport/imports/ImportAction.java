package org.testin.importExport.imports;

import com.intellij.icons.AllIcons;
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
import org.testin.enums.TestEditorAttributes;
import org.testin.generateJavaCode.autoGenerator.CreateTestMethod;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.nodeCreator.CreateTestSet;
import org.testin.util.EditorUtil;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ImportAction extends DumbAwareAction {

    protected final List<TestEditorAttributes> importAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportable)
            .toList();

    final @NotNull SimpleTree tree;

    public ImportAction(final @NotNull SimpleTree tree) {
        super("Import", "Import test cases from a file", AllIcons.ToolbarDecorator.Import);
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
                !(dirDto instanceof TestSetDirectoryDto ||
                        dirDto instanceof TestSetPackageDirectoryDto ||
                        dirDto instanceof TestCasesMainDirectoryDto)) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "Please select a valid Test Set, Test Set Package, or Test Cases Directory.");
            return;
        }

        String dirPathStr = dirDto.getPath().toString().replace('\\', '/');
        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(dirPathStr);

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "The selected path in the Project Panel is invalid.");
            return;
        }

        ImportDialog dialog = new ImportDialog(project, importAttributes, (file, format) -> format.importToFile(project, ImportAction.this, file));

        if (dialog.showAndGet()) {
            Map<String, List<TestCaseDto>> selectedCasesBySheet = dialog.getSelectedTestCasesBySheet();

            if (selectedCasesBySheet.isEmpty()) {
                Services.getInstance(project, Notifier.class).softShow(project, "No Selection", "No test cases were selected for import.");
                return;
            }

            executeImportWriteAction(project, targetDirectory, dirDto, parentNode, dialog, selectedCasesBySheet);
        } else {
            Services.getInstance(project, Notifier.class).softShow(project, "Import Cancelled", "Import was cancelled from preview dialog.");
        }
    }

    private void executeImportWriteAction(final @NotNull Project project, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode, final ImportDialog dialog, final Map<String, List<TestCaseDto>> selectedCasesBySheet) {

        ApplicationManager.getApplication().runWriteAction(() -> {
            if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                TestCaseDto tail = findExistingTail(project, targetDirectory);
                List<TestCaseDto> flatList = new ArrayList<>();
                selectedCasesBySheet.values().forEach(flatList::addAll);

                linkAndSaveTestCases(project, targetDirectory, flatList, tail);

                if (dialog.getCg().isSelected()) {
                    Logger.info("Import: generating test methods for " + flatList.size() + " imported cases");
                    CreateTestMethod syncInjector = new CreateTestMethod();
                    for (TestCaseDto tc : flatList) {
                        tc.setParent(ts);
                        List<String> fqcn = Services.getInstance(project, Tools.class).buildFqcnMethod(tc);
                        syncInjector.executeSync(project, tc, fqcn);
                    }
                }

                Services.getInstance(project, EditorUtil.class).closeThenOpen(project, targetDirectory, ts);
                Services.getInstance(project, Notifier.class).info(project, "Import Complete", "Successfully imported " + flatList.size() + " test cases.");

            } else {
                int totalImported = 0;
                for (Map.Entry<String, List<TestCaseDto>> entry : selectedCasesBySheet.entrySet()) {
                    String rawSheetName = entry.getKey();
                    List<TestCaseDto> sheetCases = entry.getValue();

                    String cName = Services.getInstance(project, Tools.class).removeSpecialChars(rawSheetName);
                    Path newDirPath = Path.of(targetDirectory.getPath()).resolve(cName);
                    DirectoryDto dir = new CreateTestSet().execute(tree, project, cName, parentNode, selectedDirDto, newDirPath);

                    // todo, to be enhanced later
                    VirtualFile sheetDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(newDirPath);

                    TestCaseDto tail = findExistingTail(project, sheetDir);
                    linkAndSaveTestCases(project, sheetDir, sheetCases, tail);

                    if (dialog.getCg().isSelected()) {
                        TestSetDirectoryDto sheetDto = (TestSetDirectoryDto) dir;

                        Logger.info("Import: generating test methods for sheet '" + cName + "' with " + sheetCases.size() + " cases");
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

        });
    }

    private void linkAndSaveTestCases(final @NotNull Project project, final VirtualFile dir, final List<TestCaseDto> testCases, final TestCaseDto existingTail) {
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
    private TestCaseDto findExistingTail(final @NotNull Project project, final VirtualFile directory) {
        if (directory == null) return null;
        VirtualFile[] children = directory.getChildren();
        if (children != null) {
            for (VirtualFile child : children) {
                if (!child.isDirectory() && child.getName().endsWith(".json")) {
                    try (InputStream is = child.getInputStream()) {
                        TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(is, TestCaseDto.class);
                        if (tc.getNext() == null) {
                            return tc;
                        }
                    } catch (final Exception ex) {
                        Logger.error("Failed to read existing test case: " + ex.getMessage());
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
