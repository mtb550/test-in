package org.testin.creator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckedTreeNode;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.DirectoryMapper;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.RunConfigurationDialog;
import org.testin.testrun.RunConfigurationForm;
import org.testin.testrun.RunTreeCellRenderer;
import org.testin.ui.framework.SelectionTree;
import org.testin.util.EditorUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class CreateTestRun implements NodeCreator {
    private final @NotNull Project p;

    /**
     * Asynchronous creator: shows the run configuration dialog and completes on OK,
     * including its own tree refresh and editor opening. Always returns null.
     */
    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        // The tree this was started from only exists when a project is bound, so
        // nobody can click their way into the miss. It is checked because a run
        // written against no project would be a directory nothing owns.
        Services.getInstance(p, BoundTestProject.class).get().ifPresentOrElse(
                tp -> configureRun(tp.getTestCasesDirectory(), name, parentDir, newDirPath),
                () -> Logger.warn("Create test run: no test project is bound to " + p.getName()));

        return Optional.empty();
    }

    /**
     * The dialog the creator is: which test cases the run covers, chosen from
     * the bound project's own tree.
     */
    private void configureRun(final @NotNull DirectoryDto testCasesRoot, final @NotNull String name,
                              final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            final @NotNull Path testCasesPath = testCasesRoot.getPath();
            final @NotNull DefaultMutableTreeNode fullModelNode = buildDirectoryTree(testCasesPath, testCasesRoot);

            final @NotNull CheckedTreeNode root = convertToCheckedNodes(fullModelNode);

            ApplicationManager.getApplication().invokeLater(() -> {

                final @NotNull RunConfigurationForm form = new RunConfigurationForm(name);
                final @NotNull SelectionTree selection = new SelectionTree(root, RunTreeCellRenderer.create(Collections.emptyMap()));

                new RunConfigurationDialog(p, form, selection, () -> {
                    // The popup is not modal - the tree stays live while the
                    // dialog is open, so the parent may have been removed.
                    if (!Services.getInstance(p, ProjectIndexer.class).nodeExists(parentDir.getPath())) {
                        Services.getInstance(p, Notifier.class).softShow(p, "'" + parentDir.getName() + "' no longer exists - test run not created");
                        return;
                    }

                    final @NotNull TestRunDirectoryDto tr = Services.getInstance(p, DirectoryMapper.class).setTestRunNode(p, newDirPath, parentDir);
                    saveSelectedToJSON(form, selection, newDirPath, Services.getInstance(p, ExplorerPanel.class), tr);
                }).show();
            });
        });
    }

    /**
     * The node for a folder, with its test cases or its child folders under it.
     * <p>
     * Always returns a node. It used to return null for an empty test set or an
     * empty container, and take an isRoot flag to exempt the top of the tree.
     * That gave one method two contracts: the root could never be null and a
     * child routinely was.
     * <p>
     * The caller below drops the empties instead - the same behavior, one rule.
     */
    private @NotNull DefaultMutableTreeNode buildDirectoryTree(final @NotNull Path folder,
                                                               final @NotNull DirectoryDto thisNodeDto) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        indexer.awaitIndexing();

        final @NotNull DefaultMutableTreeNode node = new DefaultMutableTreeNode(thisNodeDto);

        if (thisNodeDto instanceof TestSetDirectoryDto) {
            for (final TestCaseDto tc : indexer.getTestCasesForTestSet(folder)) {
                node.add(new DefaultMutableTreeNode(tc));
            }
            return node;
        }

        for (final DirectoryDto child : indexer.getChildren(folder)) {
            // A deprecated test set, or anything under an archived package, is not
            // offered for a new run: that is what retiring it means (#68).
            if (child.isRetired()) continue;

            final @NotNull DefaultMutableTreeNode childNode = buildDirectoryTree(child.getPath(), child);

            // An empty test set, or a package holding only empty ones, would
            // clutter the tree with a branch that cannot be ticked.
            if (childNode.getChildCount() > 0) node.add(childNode);
        }

        return node;
    }

    private void saveSelectedToJSON(final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull Path savePath, final @NotNull ExplorerPanel pp, final @NotNull TestRunDirectoryDto trDir) {
        final @NotNull TestRunDto tr = new TestRunDto()
                .setCreatedBy(Services.getInstance(p, AppSettingsState.class).testerName)
                .setChangeLog(form.getChangeLog().getText().trim())
                .setCommitId(form.getCommitIdField().getText().trim())
                .setPlatform(form.getFieldValue(TestRunConfiguration.PLATFORM))
                .setComponent(form.getFieldValue(TestRunConfiguration.COMPONENT))
                .setTestType(form.getFieldValue(TestRunConfiguration.TEST_TYPE))
                .setLanguage(form.getFieldValue(TestRunConfiguration.LANGUAGE))
                .setBrowser(form.getFieldValue(TestRunConfiguration.BROWSER))
                .setDeviceType(form.getFieldValue(TestRunConfiguration.DEVICE_TYPE));

        final @NotNull List<TestRunItems> items = new ArrayList<>();
        selection.forEachChecked(checked -> {
            if (checked instanceof TestCaseDto tc) {
                final @NotNull TestRunItems item = new TestRunItems();
                item.setId(tc.getId());
                item.setStatus(TestStatus.PENDING);
                items.add(item);
            }
        });
        tr.setResults(items);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(p, ProjectIndexer.class).putTestRun(savePath, tr);

            // Defaults are correct (status CREATED); addTestRunDir stamps the
            // tester's audit info before the marker's first write.
            TestRunMarker marker = new TestRunMarker();
            trDir.setMarker(marker);

            Services.getInstance(p, ProjectIndexer.class).addTestRunDir(trDir);
            Services.getInstance(p, ProjectIndexer.class).updateRunMarker(p, savePath, marker);

            // File access is the indexer's alone (see CLAUDE.md).
            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(savePath);

            ApplicationManager.getApplication().invokeLater(() -> {
                pp.getProjectTree().refresh();
                Services.getInstance(p, EditorUtil.class).openIfNotOpen(p, trDir);

                // Here rather than in CreateTreeNodeAction: creating a run is
                // asynchronous, and the action returns while the dialog is still
                // open (#62).
                Services.getInstance(p, Notifier.class).softShow(p, "Run created");
            });

        });
    }

    private @NotNull CheckedTreeNode convertToCheckedNodes(final @NotNull DefaultMutableTreeNode node) {
        final @NotNull Object userObj = node.getUserObject();
        final @NotNull CheckedTreeNode newNode = new CheckedTreeNode(userObj);
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }

}

