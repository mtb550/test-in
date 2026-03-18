package testGit.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.projectPanel.tree.TreeContextMenu;
import testGit.util.KeyboardSet;

import java.awt.*;

public class OpenNodeCM extends DumbAwareAction {
    private final SimpleTree tree;
    private final TreeContextMenu treeContextMenu;

    public OpenNodeCM(SimpleTree tree, TreeContextMenu treeContextMenu) {
        super("Show Context Menu");
        this.tree = tree;
        this.treeContextMenu = treeContextMenu;
        this.registerCustomShortcutSet(KeyboardSet.ContextMenu.getShortcut(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        int[] selectedRows = tree.getSelectionRows();
        if (selectedRows != null && selectedRows.length > 0) {
            Rectangle rect = tree.getRowBounds(selectedRows[0]);
            if (rect != null) {
                ActionManager.getInstance()
                        .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, treeContextMenu)
                        .getComponent()
                        .show(tree, rect.x + (rect.width / 2), rect.y + (rect.height / 2));
            }
        }
    }
}