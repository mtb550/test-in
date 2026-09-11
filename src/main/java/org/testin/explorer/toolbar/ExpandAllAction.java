package org.testin.explorer.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.util.Bundle;

public class ExpandAllAction extends AbstractTreeAction {

    public ExpandAllAction(final @NotNull TreePanel tp) {
        super(tp, Bundle.message("toolbar.expand.all"), Bundle.message("toolbar.expand.all.description"), AllIcons.Actions.Expandall, TreeUtil::expandAll);
    }
}
