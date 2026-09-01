package org.testin.testrun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.creator.CreateTestRun;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import javax.swing.tree.TreePath;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The next cycle, from the one before it.
 * <p>
 * A test cycle is a test run, and cycle 2 usually covers what cycle 1 covered.
 * Rebuilding that by hand means re-ticking every case in the selection tree and
 * retyping every configuration answer - slow on a run of any size, and a missed
 * tick silently changes what the cycle was for (#9).
 * <p>
 * This opens the same dialog creating a run opens, holding the previous cycle's
 * cases and its configuration, with a name suggested. Nothing else is carried:
 * the run is written by the same path a new one is, which builds its items from
 * the ticked cases and gives it a fresh marker - so no verdict, duration or
 * stack trace from the last cycle can reach this one.
 */
public class ReCreateTestRunAction extends AbstractProjectTreeAction {

    public ReCreateTestRunAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Re-create", "Create the next cycle from this test run", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Optional.ofNullable(tree.getSelectionPath()).ifPresent(this::reCreateAt);
    }

    /**
     * The run and the folder it sits in, read off the same path - the parent is
     * taken from the tree rather than from the node, which carries it as a
     * field that may not be set.
     */
    private void reCreateAt(final @NotNull TreePath path) {
        TreeValueUtil.directoryAt(path)
                .filter(TestRunDirectoryDto.class::isInstance)
                .ifPresent(source -> TreeValueUtil.directoryAt(path.getParentPath())
                        .ifPresent(parent -> reCreate(source, parent)));
    }

    private void reCreate(final @NotNull DirectoryDto source, final @NotNull DirectoryDto parent) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        final @NotNull TestRunDto run = indexer.getTestRunByPath(source.getPath());
        final @NotNull Set<UUID> cases = run.getResults().stream().map(TestRunItems::getId).collect(Collectors.toSet());

        final @NotNull Set<String> taken = indexer.getChildren(parent.getPath()).stream()
                .map(DirectoryDto::getName)
                .collect(Collectors.toSet());

        // A case removed since the source run is simply not in the tree, so it
        // is not ticked and not carried. Nothing to report and nothing to skip:
        // the tree is built from what exists now.
        Services.getInstance(p, BoundTestProject.class).get().ifPresentOrElse(
                tp -> new CreateTestRun(p).configureRun(tp.getTestCasesDirectory(), NextRunName.after(source.getName(), taken), parent, cases, run.getConfiguration()),
                () -> Logger.warn("Re-create test run: no test project is bound to " + p.getName()));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValueUtil.singleSelectedDirectory(tree).filter(TestRunDirectoryDto.class::isInstance).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the tree's selection, which is Swing state.
        return ActionUpdateThread.EDT;
    }
}
