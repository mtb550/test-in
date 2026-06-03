package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Log;

public class UndoNode extends DumbAwareAction {
    public UndoNode(SimpleTree tree) {
        super("Undo", "Undo last action", AllIcons.Actions.Undo);
        this.registerCustomShortcutSet(KeyboardSet.Undo.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Log.info("Tree Undo triggered");
    }
}