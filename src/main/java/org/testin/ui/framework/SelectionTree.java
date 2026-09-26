/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui.framework;

import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import java.util.function.Consumer;

public final class SelectionTree implements DialogComponent {
    private static final int VISIBLE_ROWS = 8;

    private final @NotNull CheckboxTree tree;
    private final @NotNull CheckedTreeNode root;
    private final @NotNull JComponent panel;

    public SelectionTree(final @NotNull String caption, final @NotNull CheckedTreeNode root, final @NotNull CheckboxTree.CheckboxTreeCellRenderer renderer) {
        this.root = root;
        tree = new CheckboxTree(renderer, root, new CheckboxTreeBase.CheckPolicy(true, true, true, true));
        // Rule-INTERNAL-095
        tree.setFont(Fonts.row());
        // Rule-INTERNAL-102
        tree.setVisibleRowCount(VISIBLE_ROWS);
        TreeUtil.expandAll(tree);

        // Rule-INTERNAL-087
        panel = Caption.above(caption, new JBScrollPane(tree));
    }

    public void forEachChecked(final @NotNull Consumer<Object> visitor) {
        visitChecked(root, visitor);
    }

    public void forEachLeaf(final @NotNull Consumer<Object> visitor) {
        visitLeaves(root, visitor);
    }

    public boolean hasChecked() {
        return hasCheckedLeaf(root);
    }

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

    private void visitLeaves(final @NotNull CheckedTreeNode node, final @NotNull Consumer<Object> visitor) {
        if (node.isLeaf()) visitor.accept(node.getUserObject());

        for (int i = 0; i < node.getChildCount(); i++) {
            visitLeaves((CheckedTreeNode) node.getChildAt(i), visitor);
        }
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
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
