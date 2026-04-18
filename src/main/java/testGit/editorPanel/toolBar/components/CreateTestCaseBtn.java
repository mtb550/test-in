package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import testGit.util.KeyboardSet;

public class CreateTestCaseBtn extends AbstractButton implements IToolbarItem {

    public CreateTestCaseBtn(final Runnable onToolBarCreateTestCaseClicked) {
        //super("Add Test Case", AllIcons.General.Add);
        super(null, AllIcons.General.Add);

        new HelpTooltip()
                .setTitle("Create test case")
                .setShortcut(KeyboardSet.CreateTestCase.getShortcut())
                .installOn(this);

        addActionListener(e -> onToolBarCreateTestCaseClicked.run());
    }
}