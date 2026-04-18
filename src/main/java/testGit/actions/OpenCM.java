package testGit.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;

import java.awt.*;

public class OpenCM extends DumbAwareAction {
    private final SimpleTree tree;
    private final JBList<?> list;
    private final DefaultActionGroup cm;

    public OpenCM(final SimpleTree tree, final DefaultActionGroup cm) {
        super("Show Context Menu");
        this.tree = tree;
        this.cm = cm;
        this.list = null;
        this.registerCustomShortcutSet(KeyboardSet.OpenContextMenu.getCustomShortcut(), tree);
    }

    public OpenCM(final JBList<?> list, final DefaultActionGroup cm) {
        super("Show Context Menu");
        this.list = list;
        this.cm = cm;
        this.tree = null;
        this.registerCustomShortcutSet(KeyboardSet.OpenContextMenu.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree != null && cm != null) {
            int[] selectedRows = tree.getSelectionRows();
            if (selectedRows != null && selectedRows.length > 0) {
                Rectangle rect = tree.getRowBounds(selectedRows[0]);
                if (rect != null) {
                    ActionManager.getInstance()
                            .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, cm)
                            .getComponent()
                            .show(tree, rect.x + (rect.width / 2), rect.y + (rect.height / 2));

                }
            }
            return;
        }

        if (list != null && cm != null) {
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex != -1) {
                Rectangle rect = list.getCellBounds(selectedIndex, selectedIndex);
                if (rect != null) {
                    ActionManager.getInstance()
                            .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, cm)
                            .getComponent()
                            .show(list, rect.x + (rect.width / 4), rect.y + (rect.height / 2));
                }
            }
        }
    }
}