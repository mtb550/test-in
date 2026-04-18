package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class PasteNode extends DumbAwareAction {
    private final SimpleTree tree;

    public PasteNode(final SimpleTree tree) {
        super("Paste", "Paste items", AllIcons.Actions.MenuPaste);
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.PasteNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Action action = tree.getActionMap().get("paste");
        if (action != null) {
            action.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, "paste"));
        }
    }

}
