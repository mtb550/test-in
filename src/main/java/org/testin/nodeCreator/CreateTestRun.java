package org.testin.nodeCreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.testin.enums.TestRunConfiguration;
import org.testin.enums.TestStatus;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.mappers.markers.MarkerMapper;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;
import org.testin.settings.AppSettingsState;
import org.testin.testRun.CreateTestRunDialog;
import org.testin.util.EditorUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateTestRun implements NodeCreator {
    private final @NotNull Project p;
    private TestRunDirectoryDto tr;

    public CreateTestRun(final @NotNull Project p) {
        this.p = p;
    }

    @Override
    public @NonNull DirectoryDto execute(final @NonNull SimpleTree tree, final @NonNull String name, final @NonNull DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final @NonNull Path newDirPath) {
        final TestProjectDirectoryDto tp = Services.getInstance(p, ProjectPanel.class).getTestProjectSelector().getSelectedTestProject().getItem();

        final DirectoryDto testCasesRoot = tp.getTestCasesDirectory();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            final Path testCasesPath = testCasesRoot.getPath();
            final DefaultMutableTreeNode fullModelNode = buildDirectoryTree(testCasesPath, true, testCasesRoot);

            final CheckedTreeNode root = convertToCheckedNodes(fullModelNode);

            ApplicationManager.getApplication().invokeLater(() -> {

                final CreateTestRunDialog form = new CreateTestRunDialog(name, root, Collections.emptyMap());

                DialogBuilder dialogBuilder = new DialogBuilder(p);
                dialogBuilder.setTitle("Create Test Run");
                dialogBuilder.setCenterPanel(form.getMainPanel());
                dialogBuilder.addOkAction().setText("Save Test Run");
                dialogBuilder.addCancelAction();

                dialogBuilder.setOkOperation(() -> {
                    dialogBuilder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
                    tr = Services.getInstance(p, DirectoryMapper.class).setTestRunNode(p, newDirPath, parentDir);
                    saveSelectedToJSON(form, root, newDirPath, Services.getInstance(p, ProjectPanel.class), tr);
                });

                dialogBuilder.show();
            });
        });

        return tr;
    }

    private DefaultMutableTreeNode buildDirectoryTree(final Path folder, final boolean isRoot, final DirectoryDto thisNodeDto) {
        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        indexer.awaitIndexing();

        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(thisNodeDto);

        if (thisNodeDto == null) return node;

        final boolean isTestSet = thisNodeDto instanceof TestSetDirectoryDto;

        if (isTestSet) {
            final List<TestCaseDto> testCases = indexer.getTestCasesForTestSet(folder);
            if (testCases.isEmpty()) return null;
            for (final TestCaseDto tc : testCases) {
                node.add(new DefaultMutableTreeNode(tc));
            }
        } else {
            final List<DirectoryDto> children = indexer.getChildren(folder);
            for (final DirectoryDto child : children) {
                final DefaultMutableTreeNode childNode = buildDirectoryTree(child.getPath(), false, child);
                if (childNode != null) node.add(childNode);
            }
            // Hide empty containers (e.g. a test-set package with no test sets/cases) so they don't clutter the tree.
            if (node.getChildCount() == 0 && !isRoot) return null;
        }

        return node;
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


    private void saveSelectedToJSON(final CreateTestRunDialog form, final CheckedTreeNode root, final Path savePath, final ProjectPanel pp, final TestRunDirectoryDto trDir) {
        final TestRunDto tr = new TestRunDto()
                .setCreatedBy(Services.getInstance(p, AppSettingsState.class).testerName)
                .setChangeLog(form.getChangeLog().getText().trim())
                .setCommitId(form.getCommitIdField().getText().trim())
                .setPlatform(form.getFieldValue(TestRunConfiguration.PLATFORM))
                .setComponent(form.getFieldValue(TestRunConfiguration.COMPONENT))
                .setTestType(form.getFieldValue(TestRunConfiguration.TEST_TYPE))
                .setLanguage(form.getFieldValue(TestRunConfiguration.LANGUAGE))
                .setBrowser(form.getFieldValue(TestRunConfiguration.BROWSER))
                .setDeviceType(form.getFieldValue(TestRunConfiguration.DEVICE_TYPE));

        final List<TestRunItems> items = new ArrayList<>();
        collectCheckedItems(root, items);
        tr.setResults(items);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(p, ProjectIndexer.class).putTestRun(savePath, tr);

            TestRunMarker marker = Services.getInstance(p, MarkerMapper.class).setTestRunMarker();
            trDir.setMarker(marker);

            Services.getInstance(p, ProjectIndexer.class).addTestRunDir(trDir);
            Services.getInstance(p, ProjectIndexer.class).updateRunMarker(p, savePath, marker);

            VirtualFile virtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(savePath.toFile());
            if (virtualDir != null)
                virtualDir.refresh(false, true);

            ApplicationManager.getApplication().invokeLater(() -> {
                pp.getTestRunTreeBuilder().buildTree(pp.getTestProjectSelector().getSelectedTestProject().getItem());
                Services.getInstance(p, EditorUtil.class).openIfNotOpen(p, trDir);

            });

        });
    }

    private @NotNull CheckedTreeNode convertToCheckedNodes(final DefaultMutableTreeNode node) {
        final Object userObj = node.getUserObject();
        final CheckedTreeNode newNode = new CheckedTreeNode(userObj);
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }

}