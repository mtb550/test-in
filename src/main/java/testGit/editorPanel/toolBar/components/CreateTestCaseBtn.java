package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class CreateTestCaseBtn extends AbstractButton implements IToolbarItem {

    public CreateTestCaseBtn(final Runnable onToolBarCreateTestCaseClicked) {
        super("Add Test Case", AllIcons.General.Add);

        addActionListener(e -> onToolBarCreateTestCaseClicked.run());
    }
}