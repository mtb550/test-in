package org.testin.order;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.Optional;

/**
 * Gives a node its place among its siblings: a number the tester types.
 * <p>
 * Nodes with a number come first, smallest first; the rest follow by the date
 * they were created, which is the order a folder has always read in. Two nodes
 * with the same number is not a problem - the date decides - so a node can be
 * put third without renumbering anything.
 * <p>
 * Declared in {@code plugin.xml} (#119), which is what puts it in Find Action
 * and lets a tester give it a key of their own in Settings -> Keymap - it has
 * never had one, and until now there was nowhere to ask for it. That is also why
 * it has no constructor and no fields: the platform builds one instance for the
 * whole IDE, so the node comes from the keystroke.
 */
public class OrderNodeAction extends DumbAwareAction {

    // UC-TREE-PANEL-015
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        orderable(e).ifPresent(node -> new OrderDialog(p, node.getOrder(), order -> apply(p, node, order)).show());
    }

    /**
     * UC-TREE-PANEL-015, Rule-TREE-PANEL-055.
     * <p>
     * Ordered is said when the node moved, and not when it did not. Re-typing
     * the number a node already has writes the same marker and used to raise the
     * same message, which reads as an answer to a change nobody made (#193).
     */
    private void apply(final @NotNull Project p, final @NotNull DirectoryDto node, final int order) {
        if (node.getOrder() == order) return;

        node.getMarker().setOrder(order);
        Services.getInstance(p, ProjectIndexer.class).persistMarker(node);

        // The tree is drawn from the children index, which sorts on the way out
        // - so what redraws it is a refresh, not a re-index.
        Services.getInstance(p, TreePanel.class).getProjectTree().refresh();
        Services.getInstance(p, Notifier.class).softShow(p, Done.ORDERED);
    }

    /**
     * The selected node, when it is a kind that can be ordered.
     * <p>
     * Empty means one of three things, and none of them needs telling apart: the
     * node is one with no arrangement to have - a project, or one of its two
     * containers - several are selected, or the keystroke never arrived in the
     * Testin tree at all. Each of them is a reason to be gray, which is the only
     * thing the two callers do with the answer.
     */
    private @NotNull Optional<DirectoryDto> orderable(final @NotNull AnActionEvent e) {
        // One node, so several selected grays the entry rather than ordering the
        // first and passing over the rest in silence (#192).
        return TestinData.singleSelectedNode(e).filter(DirectoryDto::isOrderable);
    }

    /**
     * UC-TREE-PANEL-015, Rule-TREE-PANEL-058.
     * <p>
     * Always on the menu, and greyed out on a node that has no order to set.
     * <p>
     * Hiding it would answer a question the tester did not ask: an entry that
     * appears on some nodes and not others reads as a menu that changes shape,
     * and they have to find out by right-clicking around which nodes have it.
     * Greyed out says the same thing in place - this exists, not for this one.
     * <p>
     * It is the guard on the key as well now: outside the Testin tree there is no
     * node to order, so a key bound to this is gray in a Java file (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(orderable(e).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the tree's selection, which is Swing state (#52).
        return ActionUpdateThread.EDT;
    }
}
