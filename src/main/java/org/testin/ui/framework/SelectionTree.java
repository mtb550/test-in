package org.testin.ui.framework;

import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * A checkbox tree of selectable items — the component of the run-creation
 * dialog. Space toggles the focused row; checking a branch checks its
 * children. The dialog reads the selection by visiting the checked nodes.
 */
public final class SelectionTree implements IDialogComponent {

    private final @NotNull CheckboxTree tree;
    private final @NotNull CheckedTreeNode root;
    private final @NotNull JBScrollPane panel;

    public SelectionTree(final @NotNull CheckedTreeNode root, final @NotNull CheckboxTree.CheckboxTreeCellRenderer renderer) {
        this.root = root;
        tree = new CheckboxTree(renderer, root, new CheckboxTreeBase.CheckPolicy(true, true, true, true));
        TreeUtil.expandAll(tree);

        panel = new JBScrollPane(tree);
    }

    /** Visits every checked leaf's user object, depth first. */
    public void forEachChecked(final @NotNull Consumer<Object> visitor) {
        visitChecked(root, visitor);
    }

    /** True when at least one leaf is checked. */
    public boolean hasChecked() {
        return hasCheckedLeaf(root);
    }

    /** Runs the listener whenever any row's check state changes. */
    public void onCheckChanged(final @NotNull Runnable listener) {
        tree.addCheckboxTreeListener(new CheckboxTreeListener() {
            @Override
            public void nodeStateChanged(final @NotNull CheckedTreeNode node) {
                listener.run();
            }
        });
    }

    private boolean hasCheckedLeaf(final @NotNull CheckedTreeNode node) {
        if (node.isLeaf() && node.isChecked()) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (hasCheckedLeaf((CheckedTreeNode) node.getChildAt(i))) return true;
        }
        return false;
    }

    private void visitChecked(final @NotNull CheckedTreeNode node, final @NotNull Consumer<Object> visitor) {
        if (node.isLeaf() && node.isChecked()) {
            visitor.accept(node.getUserObject());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            visitChecked((CheckedTreeNode) node.getChildAt(i), visitor);
        }
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return tree;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Checking rows is not a submit gesture; the declared keys confirm.
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
