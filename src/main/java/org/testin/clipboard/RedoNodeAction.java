package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Logger;

public class RedoNodeAction extends DumbAwareAction {
    public RedoNodeAction(final @NotNull SimpleTree tree) {
        super("Redo", "Redo last action", AllIcons.Actions.Redo);
        this.registerCustomShortcutSet(KeyboardSet.Redo.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Logger.info("Tree Redo triggered");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
