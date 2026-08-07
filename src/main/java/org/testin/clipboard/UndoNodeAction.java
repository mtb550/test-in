package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Logger;

public class UndoNodeAction extends DumbAwareAction {
    private final @NotNull Project p;

    public UndoNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Undo", "Undo last action", AllIcons.Actions.Undo);
        this.p = p;
        this.registerCustomShortcutSet(KeyboardSet.Undo.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Logger.info("Tree Undo triggered");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
