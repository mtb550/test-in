package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class RefreshBtn extends AbstractButton implements IToolbarItem {

    public RefreshBtn(final Runnable onToolBarRefreshClicked) {
        super("Refresh", AllIcons.Actions.Refresh);

        addActionListener(e -> onToolBarRefreshClicked.run());
    }
}