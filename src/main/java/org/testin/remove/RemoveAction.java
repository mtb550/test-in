package org.testin.remove;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.indexer.NodeCounter;
import org.testin.indexer.ProjectIndexer;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoHistories;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.editor.TestinEditors;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * UC-TREE-PANEL-012.
 * <p>
 * Declared in {@code plugin.xml} (#119), so Find Action offers it by name and
 * the Keymap lists it. No constructor and no fields: the platform builds one
 * instance for the whole IDE, so the nodes come from the keystroke.
 * <p>
 * <b>And declared with no default key.</b> DELETE is the tree's key, the card
 * list's and the grid's, and a registered shortcut is dispatched before a
 * component's input map - so one keymap entry would answer for all three. The
 * tree puts DELETE on this action itself, which is what Delete Test Case already
 * does on the two editors' lists.
 * <p>
 * The removal is in {@link Work}, which is what a keystroke that arrived in the
 * tree with a project behind it has to work with.
 */
public class RemoveAction extends DumbAwareAction {

    /**
     * Every selected node that can actually be removed - the tree's fixed shape
     * is not among them.
     */
    private static @NotNull List<DirectoryDto> removableNodes(final @NotNull AnActionEvent e) {
        return TestinData.selectedNodes(e).stream()
                .filter(DirectoryDto::isRemovable)
                .toList();
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-038
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        final @NotNull List<DirectoryDto> nodesToRemove = removableNodes(e);
        if (nodesToRemove.isEmpty()) return;

        new Work(p).confirm(nodesToRemove);
    }

    /**
     * UC-TREE-PANEL-012, Rule-TREE-PANEL-042.
     * <p>
     * Gray where nothing selected can go - and outside the Testin tree, where
     * nothing is selected at all, which is what keeps DELETE the tree's own key
     * rather than one that fires anywhere (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!removableNodes(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * Removing a selection of nodes, for a project that is there.
     */
    private record Work(@NotNull Project p) {

        // UC-TREE-PANEL-012, Rule-TREE-PANEL-038
        private void confirm(final @NotNull List<DirectoryDto> nodesToRemove) {
            // What it holds goes in the message, under the question. The row below
            // carries the path, captioned "From", and a second captioned row would
            // read as a destination. A test project takes every test set, case and
            // run inside it.
            final @NotNull String holds = nodesToRemove.size() == 1
                    ? NodeCounter.childCounts(p, nodesToRemove.getFirst()).describe()
                    : "";

            final @NotNull String msg = (nodesToRemove.size() == 1
                    ? Bundle.message("remove.confirm.one", nodesToRemove.getFirst().getName())
                    : Bundle.message("remove.confirm.many", String.valueOf(nodesToRemove.size())))
                    + (holds.isEmpty() ? "" : System.lineSeparator() + holds);

            // Single node: its path shows exactly what is being deleted. Several, and
            // there is no one path to show, which the dialog reads as no From row.
            final @NotNull String from = nodesToRemove.size() == 1 ? nodesToRemove.getFirst().getPath().toString() : "";
            new ConfirmDialog(p, Bundle.message("remove.confirm.title"), msg, from, "", Bundle.message("remove.confirm.button"), () -> removeNodes(nodesToRemove)).show();
        }

        // UC-TREE-PANEL-012, Rule-TREE-PANEL-041
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
                    Services.getInstance(p, TestinEditors.class).close(p, node);

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

                // At the moment it happens, not when the tester presses CTRL+Z and
                // finds out. A removal whose copy could not be kept aside is still a
                // removal; what it is not is undoable, and that is the half nothing
                // used to say (#196).
                final int lost = count - kept.size();
                if (lost > 0) {
                    Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("remove.not.undoable.title"),
                            lost == 1
                                    ? Bundle.message("remove.not.undoable.one")
                                    : Bundle.message("remove.not.undoable.many", String.valueOf(lost)));
                }
            });
        }

        /**
         * UC-TREE-PANEL-012, Rule-TREE-PANEL-042.
         * <p>
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

                    Services.getInstance(p, TreePanel.class).getProjectTree().updateNodes();
                    whenAllGone.accept(removed.get());
                });
            }
        }

        /**
         * Records what a removal would take to reverse, on the tree's own history -
         * the surface the tester was standing on when they removed it, and the one
         * they will press CTRL+Z on (#165).
         * <p>
         * Pushed even when nothing could be kept aside. The entry then puts nothing
         * back and says why - which is what the tester needs CTRL+Z to do here.
         * Pushing nothing meant the next CTRL+Z reached the change before this one
         * and took that back instead, which they never asked for (#196).
         */
        // UC-TREE-PANEL-012, Rule-TREE-PANEL-040
        private void recordRemoval(final @NotNull List<DirectoryDto> asked, final @NotNull List<Kept> kept) {
            final @NotNull String what = asked.size() == 1
                    ? Bundle.message("remove.undo.one", asked.getFirst().getName())
                    : Bundle.message("remove.undo.many", String.valueOf(asked.size()));

            Services.getInstance(p, UndoHistories.class).push(UndoScope.TREE, new UndoHistories.Operation(
                    what,
                    () -> restoreAll(kept),
                    () -> {
                        removeAll(kept);
                        return true;
                    },
                    () -> kept.forEach(one -> Services.getInstance(p, ProjectIndexer.class).forgetKept(one.copy()))));
        }

        /**
         * UC-TREE-PANEL-016, Rule-TREE-PANEL-040, Rule-INTERNAL-063.
         * <p>
         * Puts back what the removal kept aside, and answers whether all of it came
         * back. False means this has already said why, so the press is not confirmed
         * on top of it - a tester used to get "Undo Incomplete" and "Undone" from
         * one CTRL+Z, the two contradicting each other (#275).
         */
        private boolean restoreAll(final @NotNull List<Kept> kept) {
            if (kept.isEmpty()) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("remove.not.undoable.title"),
                        Bundle.message("remove.nothing.kept"));
                return false;
            }

            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            final @NotNull List<Kept> lost = kept.stream().filter(one -> !indexer.restoreNode(one.copy(), one.original())).toList();
            Services.getInstance(p, TreePanel.class).getProjectTree().updateNodes();

            if (lost.isEmpty()) return true;

            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("remove.undo.incomplete.title"), Bundle.message("remove.undo.incomplete.message", String.valueOf(lost.size()), String.valueOf(kept.size())));
            return false;
        }

        /**
         * UC-TREE-PANEL-017.
         * <p>
         * Takes them away again, through the same handler and the same waiting the
         * removal used - so the generated code, the caches and the tree see a redo
         * exactly as they saw the removal. The copies stay where they are: a redo is
         * one more press away from being undone again.
         */
        private void removeAll(final @NotNull List<Kept> kept) {
            removeEach(kept.stream().map(Kept::dto).toList(), count -> Logger.info("Removed " + count + " node(s) again."));
        }
    }

    /**
     * One node that went, and the copy that can bring it back.
     */
    private record Kept(@NotNull DirectoryDto dto, @NotNull Path original, @NotNull Path copy) {
    }
}
