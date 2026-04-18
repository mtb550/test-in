package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;

public class UndoNode extends DumbAwareAction {
    public UndoNode(SimpleTree tree) {
        super("Undo", "Undo last action", AllIcons.Actions.Undo);
        this.registerCustomShortcutSet(KeyboardSet.Undo.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("Tree Undo triggered");
    }
}