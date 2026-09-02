package org.testin.remove;

import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.NodeCounter;
import org.testin.indexer.ProjectIndexer;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoService;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.EditorUtil;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.testin.util.Shortcuts.DeletePackage;

public class RemoveAction extends AbstractProjectTreeAction {
    private final @NotNull ExplorerPanel pp;

    public RemoveAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull ExplorerPanel pp) {
        super(p, tree, "Remove", "Remove selected nodes", AllIcons.Actions.GC);
        this.pp = pp;
        this.registerCustomShortcutSet(DeletePackage.getCustomShortcut(), tree);
    }

    /**
     * @param paths Swing's own answer, which is null rather than empty when
     *              nothing is selected - see TreeValueUtil.selectedDirectories
     */
    private @NotNull List<DirectoryDto> getRemovableNodes(final TreePath @Nullable [] paths) {
        return TreeValueUtil.selectedDirectories(paths).stream()
                .filter(DirectoryDto::isRemovable)
                .toList();
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final @NotNull List<DirectoryDto> nodesToRemove = getRemovableNodes(tree.getSelectionPaths());
        if (nodesToRemove.isEmpty()) return;

        // What it holds goes in the message, under the question. The row below
        // carries the path, captioned "From", and a second captioned row would
        // read as a destination. A test project takes every test set, case and
        // run inside it.
        final @NotNull String holds = nodesToRemove.size() == 1
                ? NodeCounter.childCounts(p, nodesToRemove.getFirst()).describe()
                : "";

        final @NotNull String msg = (nodesToRemove.size() == 1
                ? "Remove '" + nodesToRemove.getFirst().getName() + "'?"
                : "Remove these " + nodesToRemove.size() + " items?")
                + (holds.isEmpty() ? "" : System.lineSeparator() + holds);

        // Single node: its path shows exactly what is being deleted. Several, and
        // there is no one path to show, which the dialog reads as no From row.
        final @NotNull String from = nodesToRemove.size() == 1 ? nodesToRemove.getFirst().getPath().toString() : "";
        new ConfirmDialog(p, "Confirm Removing", msg, from, "", "Remove", () -> removeNodes(nodesToRemove)).show();
    }

    private void removeNodes(final @NotNull List<DirectoryDto> nodesToRemove) {
        if (nodesToRemove.isEmpty()) return;

        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        // One press of CTRL+Z puts back everything this one gesture removed, so
        // what it would take is collected here and recorded once, below - not per
        // node, which is what would cost four presses to undo a selection of
        // four.
        final @NotNull List<Kept> kept = new ArrayList<>(nodesToRemove.size());

        for (final DirectoryDto node : nodesToRemove) {

            // A node with an editor open has that editor closed with it. The
            // pair tested for here is exactly the pair that declares one.
            if (node.isOpenableInEditor())
                Services.getInstance(p, EditorUtil.class).close(p, node);

            // Copied aside before it goes, because the recycle bin the removal
            // sends it to is somewhere the platform can put things and cannot
            // take them out of again. A node whose copy could not be made is
            // still removed; it is simply not part of what CTRL+Z can reach.
            indexer.keepAside(node.getPath()).ifPresent(copy -> kept.add(new Kept(node, node.getPath(), copy)));
        }

        removeEach(nodesToRemove, count -> {
            Logger.info("Removed " + count + " of " + nodesToRemove.size() + " node(s).");

            recordRemoval(nodesToRemove, List.copyOf(kept));

            if (count > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, Done.REMOVED, count);
        });
    }

    /**
     * Removes every node, and rebuilds the tree once the last of them has
     * actually gone.
     * <p>
     * The count is drained by the callbacks rather than by the loop, because
     * removal is asynchronous: a tree rebuilt when the loop ends is rebuilt
     * before a single node has been removed. Only the ones that really went are
     * counted - a fixed container reports false, and used to be reported as if
     * it were removed.
     * <p>
     * Both the removal and the redo of one come through here. The redo had its
     * own copy of the loop, without the waiting, so it rebuilt the tree before
     * anything had gone and read as a key that did nothing.
     */
    private void removeEach(final @NotNull List<DirectoryDto> nodes, final @NotNull IntConsumer whenAllGone) {
        final @NotNull AtomicInteger pending = new AtomicInteger(nodes.size());
        final @NotNull AtomicInteger removed = new AtomicInteger();

        for (final DirectoryDto node : nodes) {
            node.getType().getRemoveHandler().remove(p, node, wasRemoved -> {
                if (wasRemoved) removed.incrementAndGet();
                if (pending.decrementAndGet() != 0) return;

                pp.getProjectTree().updateNodes();
                whenAllGone.accept(removed.get());
            });
        }
    }

    /**
     * One node that went, and the copy that can bring it back.
     */
    private record Kept(@NotNull DirectoryDto dto, @NotNull Path original, @NotNull Path copy) {
    }

    /**
     * Records what a removal would take to reverse, on the tree's own history -
     * the surface the tester was standing on when they removed it, and the one
     * they will press CTRL+Z on (#165).
     * <p>
     * Only what was actually kept. A node whose copy could not be made is
     * removed exactly as it always was, and is simply not part of the operation.
     */
    private void recordRemoval(final @NotNull List<DirectoryDto> asked, final @NotNull List<Kept> kept) {
        if (kept.isEmpty()) return;

        final @NotNull String what = asked.size() == 1 ? "Remove '" + asked.getFirst().getName() + "'" : "Remove " + kept.size() + " items";

        Services.getInstance(p, UndoService.class).push(UndoScope.TREE, new UndoService.Operation(
                what,
                () -> restoreAll(kept),
                () -> removeAll(kept),
                () -> kept.forEach(one -> Services.getInstance(p, ProjectIndexer.class).forgetKept(one.copy()))));
    }

    private void restoreAll(final @NotNull List<Kept> kept) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        final @NotNull List<Kept> lost = kept.stream().filter(one -> !indexer.restoreNode(one.copy(), one.original())).toList();
        pp.getProjectTree().updateNodes();

        // What did not come back is the only thing worth saying. The tree used
        // to be refreshed either way and nothing read the answer, so an undo
        // whose copy was gone redrew exactly like one that worked - and the
        // tester was told "Undone" over a node still missing.
        if (!lost.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, "Undo Incomplete", lost.size() + " of " + kept.size() + " could not be put back");
        }
    }

    /**
     * Takes them away again, through the same handler and the same waiting the
     * removal used - so the generated code, the caches and the tree see a redo
     * exactly as they saw the removal. The copies stay where they are: a redo is
     * one more press away from being undone again.
     */
    private void removeAll(final @NotNull List<Kept> kept) {
        removeEach(kept.stream().map(Kept::dto).toList(), count -> Logger.info("Removed " + count + " node(s) again."));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!getRemovableNodes(tree.getSelectionPaths()).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
