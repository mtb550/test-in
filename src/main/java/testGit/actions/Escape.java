package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.projectPanel.tree.TreeTransferHandler;
import testGit.util.KeyboardSet;

public class Escape extends DumbAwareAction {
    private final SimpleTree tree;
    private final TreeTransferHandler transferHandler;

    public Escape(final SimpleTree tree, final TreeTransferHandler transferHandler) {
        super("Escape Action", "", AllIcons.Actions.InlayGear);
        this.tree = tree;
        this.transferHandler = transferHandler;
        this.registerCustomShortcutSet(KeyboardSet.Escape.getShortcut(), tree);

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        transferHandler.getSelectedNodes().clear();
        transferHandler.resetLastAction();
        tree.repaint();
        System.out.println("Clipboard/Cut state cleared via ESC.");
    }
}