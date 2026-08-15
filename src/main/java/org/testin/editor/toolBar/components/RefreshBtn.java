package org.testin.editor.toolBar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class RefreshBtn extends AbstractButton implements ToolbarItem {

    public RefreshBtn(final @NotNull Runnable onToolBarRefreshClicked) {
        super("Refresh", AllIcons.Actions.Refresh);

        addActionListener(e -> onToolBarRefreshClicked.run());
    }
}