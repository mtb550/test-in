package testGit.editorPanel.testRunEditor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;

public class TestRunContextMenu extends DefaultActionGroup {
    public TestRunContextMenu() {
        super("Test Run Actions", true);
        add(new AnAction("Run This Test", "Execute single test", AllIcons.Actions.Execute) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
            }
        });
        addSeparator();
        add(new AnAction("Copy ID", "Copy test identifier", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
            }
        });
    }
}