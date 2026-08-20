package org.testin.order;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.explorer.tree.TreeValueUtil;

import java.util.Optional;

/**
 * Gives a node its place among its siblings: a number the tester types.
 * <p>
 * Nodes with a number come first, smallest first; the rest follow by the date
 * they were created, which is the order a folder has always read in. Two nodes
 * with the same number is not a problem - the date decides - so a node can be
 * put third without renumbering anything.
 */
public class OrderNodeAction extends AbstractProjectTreeAction {

    private final @NotNull ExplorerPanel pp;

    public OrderNodeAction(final @NotNull Project p, final @NotNull ExplorerPanel pp, final @NotNull SimpleTree tree) {
        super(p, tree, "Order", "Set where this node sits among its siblings", AllIcons.ObjectBrowser.Sorted);
        this.pp = pp;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        selected().ifPresent(node -> new OrderDialog(p, node.getOrder(), order -> apply(node, order)).show());
    }

    private void apply(final @NotNull DirectoryDto node, final int order) {
        node.getMarker().setOrder(order);
        Services.getInstance(p, ProjectIndexer.class).persistMarker(node);

        // The tree is drawn from the children index, which sorts on the way out
        // - so what redraws it is a refresh, not a re-index.
        pp.getProjectTree().refresh();
        Services.getInstance(p, Notifier.class).softShow(p, "Ordered");
    }

    /**
     * The selected node, when there is one and it is a kind that can be ordered.
     * <p>
     * Two things can be absent here and both are ordinary: a tree with nothing
     * selected, and a row that is not a directory at all. Answering with an
     * empty Optional rather than a null puts both behind one word, so the menu
     * state and the action are each a single line and neither tests for
     * anything.
     */
    private @NotNull Optional<DirectoryDto> selected() {
        return Optional.ofNullable(TreeValueUtil.selectedDirectory(tree)).filter(DirectoryDto::isOrderable);
    }

    /**
     * Always on the menu, and greyed out on a node that has no order to set.
     * <p>
     * Hiding it would answer a question the tester did not ask: an entry that
     * appears on some nodes and not others reads as a menu that changes shape,
     * and they have to find out by right-clicking around which nodes have it.
     * Greyed out says the same thing in place - this exists, not for this one.
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(selected().isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the tree's selection, which is Swing state (#52).
        return ActionUpdateThread.EDT;
    }
}
