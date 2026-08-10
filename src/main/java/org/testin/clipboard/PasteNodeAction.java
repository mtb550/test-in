package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.ConfirmationPopupDialog;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class PasteNodeAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;

    public PasteNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Paste", "Paste items", AllIcons.Actions.MenuPaste);
        this.p = p;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.PasteNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Action action = tree.getActionMap().get("paste");
        if (action == null) return;

        new ConfirmationPopupDialog(
                p,
                "Paste",
                AllIcons.Actions.MenuPaste,
                "Paste selected items into the selected node?\n\nPress Enter to paste or Escape to cancel.",
                () -> action.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, "paste"))
        ).show();
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
