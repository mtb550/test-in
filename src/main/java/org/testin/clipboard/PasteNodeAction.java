package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.explorer.tree.TreeTransferPayload;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class PasteNodeAction extends AbstractProjectTreeAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);

    public PasteNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Paste", "Paste items", AllIcons.Actions.MenuPaste);
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (!(tree.getTransferHandler() instanceof TreeTransferHandler transferHandler)) return;

        final Transferable contents = CopyPasteManager.getInstance().getContents();
        if (contents == null || !contents.isDataFlavorSupported(TreeTransferHandler.NODE_FLAVOR)) return;

        final DirectoryDto target = TreeValueUtil.selectedDirectory(tree.getSelectionPath());
        if (target == null) return;

        try {
            final TreeTransferPayload payload = (TreeTransferPayload) contents.getTransferData(TreeTransferHandler.NODE_FLAVOR);

            // Only what can actually land on this target - family rules, plus
            // never onto itself, into its own subtree, or into its own parent.
            final List<DirectoryDto> nodes = Arrays.stream(payload.nodes())
                    .filter(node -> transferHandler.canTransferInto(node, target))
                    .toList();

            transferHandler.notifyNameCollisions(payload.nodes(), target);
            if (nodes.isEmpty()) return;

            // Cut-paste moves, copy-paste duplicates - each says what it does.
            final boolean move = payload.clipboardAction() == TransferHandler.MOVE;
            final String verb = move ? "Move" : "Copy";
            final String what = nodes.size() == 1 ? "'" + nodes.getFirst().getName() + "'" : nodes.size() + " items";
            final Path fromPath = nodes.getFirst().getPath().getParent();

            new ConfirmDialog(p, "Paste",
                    verb + " " + what + " into '" + target.getName() + "'?",
                    fromPath == null ? null : fromPath.toString(),
                    target.getPath().toString(),
                    verb,
                    transferHandler::pasteFromClipboard
            ).show();

        } catch (final Exception ex) {
            Logger.error("Paste failed: " + ex.getMessage());
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

}
