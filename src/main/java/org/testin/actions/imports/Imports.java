package org.testin.actions.imports;

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
import org.testin.actions.nodeCreator.CreateTestSet;
import org.testin.pojo.FileTypes;
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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class Imports extends DumbAwareAction {

    protected final List<String> IMPORT_COLUMNS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportable)
            .map(TestEditorAttributes::getName)
            .toList();

    final @NotNull SimpleTree tree;

    public Imports(final @NotNull SimpleTree tree) {
        super("Import", "Import test cases from a file", AllIcons.ToolbarDecorator.Import);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ImportContext ctx = validateTreeSelection(e);
        if (ctx == null) return;

        final Project project = ctx.project;
        final VirtualFile targetDirectory = ctx.targetDirectory;
        final DirectoryDto dirDto = ctx.dirDto;
        final DefaultMutableTreeNode parentNode = ctx.parentNode;

        ImportPreviewDialog dialog = new ImportPreviewDialog(project, new LinkedHashMap<>());
        dialog.setImportFileLoader((file, format) -> delegateToFormat(project, file, format));

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

    private Map<String, List<TestCaseDto>> delegateToFormat(final @NotNull Project project, final File importFile, final FileTypes format) {
        return switch (format) {
            case CSV -> new ImportCsv(Imports.this).processImport(project, importFile);
            case XLS, XLSX -> new ImportExcel(Imports.this).processImport(project, importFile);
            case JSON -> new ImportJson(Imports.this).processImport(project, importFile);
            case HTML -> {
                Services.getInstance(project, Notifier.class).warn(project, "Unsupported", "HTML import is not supported.");
                yield new LinkedHashMap<>();
            }
        };
    }

    @Nullable
    private ImportContext validateTreeSelection(final @NotNull AnActionEvent e) {
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

    private void executeImportWriteAction(final @NotNull Project project, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto, final DefaultMutableTreeNode parentNode, final ImportPreviewDialog dialog, final Map<String, List<TestCaseDto>> selectedCasesBySheet) {

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                    TestCaseDto tail = findExistingTail(project, targetDirectory);
                    List<TestCaseDto> flatList = new ArrayList<>();
                    selectedCasesBySheet.values().forEach(flatList::addAll);

                    linkAndSaveTestCases(project, targetDirectory, flatList, tail);

                    if (dialog.getCg().isSelected()) {
                        Log.info("Import: generating test methods for " + flatList.size() + " imported cases");
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

                        VirtualFile sheetDir = new CreateTestSet().inBackground(project, this, targetDirectory, selectedDirDto, parentNode, tree, rawSheetName);

                        TestCaseDto tail = findExistingTail(project, sheetDir);
                        linkAndSaveTestCases(project, sheetDir, sheetCases, tail);

                        if (dialog.getCg().isSelected()) {
                            String sheetName = sheetDir.getName();
                            TestSetDirectoryDto sheetDto = TestSetDirectoryDto.builder()
                                    .name(sheetName)
                                    .path(Path.of(sheetDir.getPath()))
                                    .path2(Services.getInstance(project, Tools.class).buildPath2(selectedDirDto.getPath2(), sheetName))
                                    .parent(selectedDirDto)
                                    .build();

                            Log.info("Import: generating test methods for sheet '" + sheetName + "' with " + sheetCases.size() + " cases");
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

    @Nullable
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

    private record ImportContext(
            @NotNull Project project,
            @NotNull VirtualFile targetDirectory,
            @NotNull DirectoryDto dirDto,
            @NotNull DefaultMutableTreeNode parentNode
    ) {
    }
}