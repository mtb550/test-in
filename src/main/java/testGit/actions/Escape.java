package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.projectPanel.tree.TreeTransferHandler;
import testGit.util.KeyboardSet;
import testGit.viewPanel.ViewToolWindowFactory;

public class Escape extends DumbAwareAction {
    private final SimpleTree tree;
    private final TreeTransferHandler transferHandler;
    private final JBList<TestCaseDto> list;

    public Escape(final SimpleTree tree, final TreeTransferHandler transferHandler) {
        super("Escape Action", "", AllIcons.Actions.InlayGear);
        this.tree = tree;
        this.transferHandler = transferHandler;
        this.list = null;
        this.registerCustomShortcutSet(KeyboardSet.Escape.getCustomShortcut(), tree);
    }

    public Escape(final JBList<TestCaseDto> list) {
        super("Escape Action", "", AllIcons.Actions.InlayGear);
        this.list = list;
        this.tree = null;
        this.transferHandler = null;
        this.registerCustomShortcutSet(KeyboardSet.Escape.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree != null && transferHandler != null) {
            transferHandler.getSelectedNodes().clear();
            transferHandler.resetLastAction();
            tree.repaint();
            System.out.println("Clipboard/Cut state cleared via ESC.");
            return;
        }

        if (list != null) {
            ToolWindow toolWindow = ViewToolWindowFactory.getToolWindow();

            if (toolWindow != null && toolWindow.isVisible()) {
                toolWindow.hide(null);
                return;
            }

            if (!list.isSelectionEmpty()) {
                list.clearSelection();
            }
        }
    }
}