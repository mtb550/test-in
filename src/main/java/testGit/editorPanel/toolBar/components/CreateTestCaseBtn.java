package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class CreateTestCaseBtn extends AbstractButton implements IToolbarItem {

    public CreateTestCaseBtn() {
        super("Add Test Case", AllIcons.General.Add);

        addActionListener(e -> {
            // TODO: "Create Test Case" call action here
            System.out.println("Add Test Case clicked");
        });
    }
}