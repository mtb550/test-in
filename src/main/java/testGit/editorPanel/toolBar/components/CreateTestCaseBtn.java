package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class CreateTestCaseBtn extends ToolbarActionButton {

    public CreateTestCaseBtn(Runnable onClickAction) {
        super("Add Test Case", AllIcons.General.Add);

        addActionListener(e -> onClickAction.run());
    }
}