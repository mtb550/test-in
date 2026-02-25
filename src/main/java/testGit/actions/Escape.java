package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.projectPanel.TransferHandlerImpl;
import testGit.util.ShortcutSet;

public class Escape extends DumbAwareAction {
    private final SimpleTree tree;
    private final TransferHandlerImpl transferHandler;

    public Escape(final SimpleTree tree, final TransferHandlerImpl transferHandler) {
        super("Escape Action", "", AllIcons.Actions.InlayGear);
        this.tree = tree;
        this.transferHandler = transferHandler;
        this.registerCustomShortcutSet(ShortcutSet.Escape.get(), tree);

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        transferHandler.getCutNodes().clear();
        transferHandler.resetLastAction();
        tree.repaint();
        System.out.println("Clipboard/Cut state cleared via ESC.");
    }
}