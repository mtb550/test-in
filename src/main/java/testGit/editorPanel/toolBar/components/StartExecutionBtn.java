package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class StartExecutionBtn extends AbstractButton implements IToolbarItem {

    public StartExecutionBtn(final Runnable onStartExecutionClicked) {
        super("Start Execution", AllIcons.Nodes.Services);
        addActionListener(e -> onStartExecutionClicked.run());

    }
}