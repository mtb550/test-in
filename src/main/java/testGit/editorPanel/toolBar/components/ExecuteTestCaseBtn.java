package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class ExecuteTestCaseBtn extends ToolbarActionButton {

    public ExecuteTestCaseBtn(Runnable onClickAction) {
        super("Execute Run", AllIcons.Nodes.Services);
        addActionListener(e -> onClickAction.run());
    }
}