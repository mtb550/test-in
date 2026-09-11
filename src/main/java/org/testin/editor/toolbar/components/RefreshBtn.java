package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import org.testin.util.Bundle;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class RefreshBtn extends AbstractIconButton implements ToolbarItem {

    // UC-EDITOR-PANEL-027
    public RefreshBtn(final @NotNull Runnable onToolBarRefreshClicked) {
        super(Bundle.message("toolbar.refresh"), AllIcons.Actions.Refresh);

        addActionListener(e -> onToolBarRefreshClicked.run());
    }
}