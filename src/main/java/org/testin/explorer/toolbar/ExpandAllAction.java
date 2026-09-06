package org.testin.explorer.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;

public class ExpandAllAction extends AbstractTreeAction {

    public ExpandAllAction(final @NotNull TreePanel tp) {
        super(tp, "Expand All", "Expand all nodes", AllIcons.Actions.Expandall, TreeUtil::expandAll);
    }
}
