package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Log;

public class RedoNode extends DumbAwareAction {
    public RedoNode(SimpleTree tree) {
        super("Redo", "Redo last action", AllIcons.Actions.Redo);
        this.registerCustomShortcutSet(KeyboardSet.Redo.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Log.info("Tree Redo triggered");
    }
}