package org.testin.explorer.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.util.Bundle;

public class CollapseAllAction extends AbstractTreeAction {

    public CollapseAllAction(final @NotNull TreePanel tp) {
        super(tp, Bundle.message("toolbar.collapse.all"), Bundle.message("toolbar.collapse.all.description"), AllIcons.Actions.Collapseall, tree -> TreeUtil.collapseAll(tree, 0));
    }
}
