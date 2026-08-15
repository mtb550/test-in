package org.testin.explorer.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.ExplorerPanel;

public class CollapseAllAction extends AbstractTreeAction {

    public CollapseAllAction(final @NotNull ExplorerPanel pp) {
        super(pp, "Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall, tree -> TreeUtil.collapseAll(tree, 0));
    }
}
