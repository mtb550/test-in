package org.testin.explorer.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;

public class CollapseAllAction extends AbstractTreeAction {

    public CollapseAllAction(final @NotNull TreePanel tp) {
        super(tp, "Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall, tree -> TreeUtil.collapseAll(tree, 0));
    }
}
