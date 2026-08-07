package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.util.KeyboardSet;

public class RedoNodeAction extends DumbAwareAction {
    private final @NotNull Project p;

    public RedoNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Redo", "Redo last action", AllIcons.Actions.Redo);
        this.p = p;
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
