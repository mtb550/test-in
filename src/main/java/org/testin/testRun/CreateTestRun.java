package org.testin.testRun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.testin.Dialogs.RunCreationForm;
import org.testin.enums.TestRunConfiguration;
import org.testin.enums.TestStatus;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.*;
import org.testin.mappers.markers.MarkerMapper;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.nodeCreator.CreateTreeNode;
import org.testin.nodeCreator.NodeCreator;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.AppSettingsState;
import org.testin.util.EditorUtil;
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

        final Object label = isRoot ? parentOfThisNode : parentOfThisNode.resolveDirectoryObject(folder, indexer);
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


    private void saveSelectedToJSON(final RunCreationForm form, final CheckedTreeNode root, final Path savePath, final ProjectPanel projectPanel, final TestRunDirectoryDto trDir) {
        final TestRunDto tr = new TestRunDto()
                .setCreatedBy(AppSettingsState.getInstance().testerName)
                .setReleaseNotes(form.getDescriptionField().getText().trim())
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
            // The indexer owns all dir/file creation: putTestRun writes the run json (creating
            // the dir) and addTestRunDir writes the .tr marker + refreshes the VFS.
            Services.getInstance(project, ProjectIndexer.class).putTestRun(savePath, tr);

            TestRunMarker marker = Services.getInstance(project, MarkerMapper.class).setTestRunMarker();
            trDir.setMarker(marker);

            Services.getInstance(project, ProjectIndexer.class).addTestRunDir(trDir);
            Services.getInstance(project, ProjectIndexer.class).updateRunMarker(project, savePath, marker);

            VirtualFile virtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(savePath.toFile());
            if (virtualDir != null)
                virtualDir.refresh(false, true);

            ApplicationManager.getApplication().invokeLater(() -> {
                projectPanel.getTestRunTreeBuilder().buildTree(projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
                Services.getInstance(project, EditorUtil.class).openIfNotOpen(project, trDir);

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