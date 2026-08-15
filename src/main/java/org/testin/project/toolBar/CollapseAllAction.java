package org.testin.project.toolBar;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.project.ProjectPanel;

public class CollapseAllAction extends AbstractTreeAction {

    public CollapseAllAction(final @NotNull ProjectPanel pp) {
        super(pp, "Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall, tree -> TreeUtil.collapseAll(tree, 0));
    }
}
