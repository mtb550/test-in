package org.testin.actions.nodeCreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.pojo.markers.MarkerMapper;
import org.testin.pojo.markers.TestRunMarker;
import org.testin.projectPanel.ProjectPanel;
import org.testin.ui.RunCreationForm;
import org.testin.util.EditorUtil;
import org.testin.util.FilesUtil;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateTestRun implements NodeCreator {
    private Project project;
    private TestRunDirectoryDto tr;

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final @NotNull Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        this.project = project;
        final TestProjectDirectoryDto tp = action.getProjectPanel().getTestProjectSelector().getSelectedTestProject().getItem();

        final DirectoryDto testCasesRoot = tp.getTestCasesDirectory();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            final Path testCasesPath = testCasesRoot.getPath();
            final DefaultMutableTreeNode fullModelNode = buildDirectoryTree(testCasesPath, true, testCasesRoot);
            final CheckedTreeNode root = convertToCheckedNodes(fullModelNode);

            ApplicationManager.getApplication().invokeLater(() -> {

                final RunCreationForm form = new RunCreationForm(name, root, Collections.emptyMap());

                DialogBuilder dialogBuilder = new DialogBuilder(project);
                dialogBuilder.setTitle("Create Test Run");
                dialogBuilder.setCenterPanel(form.getMainPanel());
                dialogBuilder.addOkAction().setText("Save Test Run");
                dialogBuilder.addCancelAction();

                dialogBuilder.setOkOperation(() -> {
                    dialogBuilder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
                    tr = Services.getInstance(project, DirectoryMapper.class).setTestRunNode(project, newDirPath, parentDir);
                    saveSelectedToJSON(form, root, newDirPath, action.getProjectPanel(), tr);
                });

                dialogBuilder.show();
            });
        });

        return tr;
    }

    private DefaultMutableTreeNode buildDirectoryTree(final Path folder, final boolean isRoot, final DirectoryDto parentOfThisNode) {
        final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
        indexer.awaitIndexing();

        final Object label = isRoot ? parentOfThisNode : resolveDirectoryObject(folder, parentOfThisNode, indexer);
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
        final DirectoryDto thisNodeDto = (label instanceof DirectoryDto) ? (DirectoryDto) label : null;

        if (thisNodeDto == null) return node;

        final boolean isTestSet = thisNodeDto instanceof TestSetDirectoryDto;

        if (isTestSet) {
            final List<TestCaseDto> testCases = indexer.getTestCasesForTestSet(folder);
            for (final TestCaseDto tc : testCases) {
                node.add(new DefaultMutableTreeNode(tc));
            }
        } else {
            final List<DirectoryDto> children = indexer.getChildren(folder);
            for (final DirectoryDto child : children) {
                node.add(buildDirectoryTree(child.getPath(), false, child));
            }
        }

        return node;
    }

    private Object resolveDirectoryObject(final Path folder, final DirectoryDto parentDir, final ProjectIndexer indexer) {
        final DirectoryDto child = indexer.getChildren(parentDir.getPath()).stream()
                .filter(dto -> dto.getPath().equals(folder))
                .findFirst()
                .orElse(null);

        if (child != null) return child;

        if (indexer.getTestSetPackageByPath(folder) != null)
            return indexer.getTestSetPackageByPath(folder);

        if (indexer.getTestSetByPath(folder) != null)
            return indexer.getTestSetByPath(folder);

        if (indexer.getTestCasesMainDirByPath(folder) != null)
            return indexer.getTestCasesMainDirByPath(folder);

        if (indexer.getTestRunDirByPath(folder) != null)
            return indexer.getTestRunDirByPath(folder);

        if (indexer.getTestRunsMainDirByPath(folder) != null)
            return indexer.getTestRunsMainDirByPath(folder);

        throw new RuntimeException("Could not resolve directory " + folder + ", parent: " + parentDir.getClass().getSimpleName());
    }

    private void collectCheckedItems(final CheckedTreeNode node, final List<TestRunItems> items) {
        if (node.getUserObject() instanceof TestCaseDto tc && node.isChecked()) {
            final TestRunItems item = new TestRunItems();
            item.setId(tc.getId());
            item.setStatus(TestStatus.PENDING);

            items.add(item);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), items);
        }
    }


    private void saveSelectedToJSON(final RunCreationForm form, final CheckedTreeNode root, final Path savePath, final ProjectPanel projectPanel, final TestRunDirectoryDto tr) {
        final TestRunDto run = new TestRunDto();
        form.populateConfiguration(run);

        final List<TestRunItems> items = new ArrayList<>();
        collectCheckedItems(root, items);
        run.setResults(items);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(project, FilesUtil.class).createDirectories(savePath);
            Services.getInstance(project, ProjectIndexer.class).putTestRun(savePath, run);

            TestRunMarker marker = Services.getInstance(project, MarkerMapper.class).setTestRunMarker();
            tr.setMarker(marker);

            Services.getInstance(project, ProjectIndexer.class).addTestRunDir(tr);
            Services.getInstance(project, ProjectIndexer.class).updateRunMarker(project, savePath, marker);

            VirtualFile virtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(savePath.toFile());
            if (virtualDir != null)
                virtualDir.refresh(false, true);

            ApplicationManager.getApplication().invokeLater(() -> {
                projectPanel.getTestRunTreeBuilder().buildTree(projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
                Services.getInstance(project, EditorUtil.class).openEditorIfNotOpen(project, tr);

            });

        });
    }

    private CheckedTreeNode convertToCheckedNodes(final DefaultMutableTreeNode node) {
        final Object userObj = node.getUserObject();
        final CheckedTreeNode newNode = new CheckedTreeNode(userObj);
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }

}