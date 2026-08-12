package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class RefreshBtn extends AbstractButton implements IToolbarItem {

    public RefreshBtn(final @NotNull Runnable onToolBarRefreshClicked) {
        super("Refresh", AllIcons.Actions.Refresh);

        addActionListener(e -> onToolBarRefreshClicked.run());
    }
}