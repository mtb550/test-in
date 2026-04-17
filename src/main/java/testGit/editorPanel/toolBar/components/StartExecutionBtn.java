package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class StartExecutionBtn extends AbstractButton implements IToolbarItem {

    public StartExecutionBtn() {
        super("Start Execution", AllIcons.Nodes.Services);

        addActionListener(e -> {
            // TODO: Gatling/TestNG action trigger, add new action class in action package
            System.out.println("Executing tests...");
        });
    }
}