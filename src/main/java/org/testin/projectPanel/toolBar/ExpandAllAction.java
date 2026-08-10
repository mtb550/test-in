package org.testin.projectPanel.toolBar;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.projectPanel.ProjectPanel;

public class ExpandAllAction extends AbstractTreeAction {

    public ExpandAllAction(final @NotNull ProjectPanel pp) {
        super(pp, "Expand All", "Expand all nodes", AllIcons.Actions.Expandall, TreeUtil::expandAll);
    }
}
