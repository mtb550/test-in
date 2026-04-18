package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class CopyNode extends DumbAwareAction {
    private final SimpleTree tree;

    public CopyNode(final SimpleTree tree) {
        super("Copy", "Copy selected items", AllIcons.Actions.Copy);
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CopyNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Action action = tree.getActionMap().get("copy");
        if (action != null) {
            action.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, "copy"));
        }
    }

}
