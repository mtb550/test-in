package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class ExecuteTestCaseBtn extends AbstractButton implements IToolbarItem {

    public ExecuteTestCaseBtn() {
        super("Start Execution", AllIcons.Nodes.Services);

        addActionListener(e -> {
            // TODO: Gatling/TestNG trigger
            System.out.println("Executing tests...");
        });
    }
}