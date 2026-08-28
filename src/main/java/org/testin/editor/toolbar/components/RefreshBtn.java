package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class RefreshBtn extends AbstractIconButton implements ToolbarItem {

    public RefreshBtn(final @NotNull Runnable onToolBarRefreshClicked) {
        super("Refresh", AllIcons.Actions.Refresh);

        addActionListener(e -> onToolBarRefreshClicked.run());
    }
}