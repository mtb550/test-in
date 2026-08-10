package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.projectPanel.tree.TreeTransferHandler;
import org.testin.ui.dialogs.ConfirmationPopupDialog;
import org.testin.util.Tools;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

public class PasteNodeAction extends DumbAwareAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;

    public PasteNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Paste", "Paste items", AllIcons.Actions.MenuPaste);
        this.p = p;
        this.tree = tree;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (!(tree.getTransferHandler() instanceof TreeTransferHandler transferHandler)) return;

        new ConfirmationPopupDialog(
                p,
                "Paste",
                AllIcons.Actions.MenuPaste,
                "Paste selected items into the selected node?\n\nPress Enter to paste or Escape to cancel.",
                transferHandler::pasteFromClipboard
        ).show();
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
