package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class RefreshBtn extends ToolbarActionButton {
    public RefreshBtn(Runnable onRefreshAction) {
        super("Refresh", AllIcons.Actions.Refresh);
        addActionListener(e -> onRefreshAction.run());
    }
}