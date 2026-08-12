package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class RedoNodeAction extends DumbAwareAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK);
    private final @NotNull Project p;

    public RedoNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Redo", "Redo last action", AllIcons.Actions.Redo);
        this.p = p;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), tree);
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
