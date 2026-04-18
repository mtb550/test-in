package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;

public class RedoNode extends DumbAwareAction {
    public RedoNode(SimpleTree tree) {
        super("Redo", "Redo last action", AllIcons.Actions.Redo);
        this.registerCustomShortcutSet(KeyboardSet.Redo.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("Tree Redo triggered");
    }
}