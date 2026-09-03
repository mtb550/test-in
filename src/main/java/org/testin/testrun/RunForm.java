package org.testin.testrun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckedTreeNode;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunConfiguration;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.ui.framework.SelectionTree;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The run configuration dialog, opened holding whatever the caller wants it to
 * hold.
 * <p>
 * Three flows open it and only one of them is creating a run: a new run opens it
 * empty, re-creating a cycle opens it on the previous cycle's cases and answers
 * (#9), and editing opens it on the run's own (#96). It used to live inside the
 * creator, which meant two of its callers reached into a class named for
 * something they were not doing - and, more to the point, that the tree of test
 * cases could only be built by whoever was creating.
 * <p>
 * What the tree shows is read from the index every time this opens, never from
 * what a run remembers. That is what makes editing worth having: a case added to
 * a test set after the run was created appears here, unticked.
 */
@AllArgsConstructor
public final class RunForm {

    private final @NotNull Project p;

    /**
     * Builds the tree off the EDT - it awaits indexing and walks the project -
     * and shows the dialog back on it.
     */
    public void open(final @NotNull DirectoryDto testCasesRoot, final @NotNull String name, final @NotNull Set<UUID> checked, final @NotNull Map<TestRunConfiguration, String> configuration, final @NotNull RunFormAction action) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            final @NotNull DefaultMutableTreeNode fullModelNode = buildDirectoryTree(testCasesRoot.getPath(), testCasesRoot);

            final @NotNull CheckedTreeNode root = convertToCheckedNodes(fullModelNode);
            if (!checked.isEmpty()) checkOnly(root, checked);

            ApplicationManager.getApplication().invokeLater(() -> {

                final @NotNull RunConfigurationForm form = new RunConfigurationForm(name);
                if (!configuration.isEmpty()) form.fillFrom(configuration);

                final @NotNull SelectionTree selection = new SelectionTree(root, RunTreeCellRenderer.create(Collections.emptyMap()));

                new RunConfigurationDialog(p, form, selection, action).show();
            });
        });
    }

    /**
     * Ticks the cases wanted, and unticks everything else.
     * <p>
     * Both halves of that, because a node arrives ticked: {@code CheckedTreeNode}
     * defaults to checked, which is why creating a run opens with the whole
     * project selected. Opening on a run's own cases means that run's scope, so
     * everything outside it has to come off.
     */
    private void checkOnly(final @NotNull CheckedTreeNode node, final @NotNull Set<UUID> wanted) {
        if (node.getUserObject() instanceof TestCaseDto tc) node.setChecked(wanted.contains(tc.getId()));

        for (int i = 0; i < node.getChildCount(); i++) {
            checkOnly((CheckedTreeNode) node.getChildAt(i), wanted);
        }
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
    private @NotNull DefaultMutableTreeNode buildDirectoryTree(final @NotNull Path folder, final @NotNull DirectoryDto thisNodeDto) {
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

    private @NotNull CheckedTreeNode convertToCheckedNodes(final @NotNull DefaultMutableTreeNode node) {
        final @NotNull Object userObj = node.getUserObject();
        final @NotNull CheckedTreeNode newNode = new CheckedTreeNode(userObj);
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }

    /**
     * The test cases ticked in the tree, which is the one thing every caller
     * wants out of it.
     */
    public static @NotNull Set<UUID> checkedCases(final @NotNull SelectionTree selection) {
        final @NotNull Set<UUID> ids = new LinkedHashSet<>();

        selection.forEachChecked(checked -> {
            if (checked instanceof TestCaseDto tc) ids.add(tc.getId());
        });

        return ids;
    }
}
