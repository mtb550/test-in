package org.testin.open;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class OpenContextMenuAction extends DumbAwareAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0);
    private final @NotNull Project p;
    private final SimpleTree tree;
    private final JBList<?> list;
    private final DefaultActionGroup cm;

    public OpenContextMenuAction(final @NotNull Project p, final SimpleTree tree, final DefaultActionGroup cm) {
        super("Show Context Menu");
        this.p = p;
        this.tree = tree;
        this.cm = cm;
        this.list = null;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), tree);
    }

    public OpenContextMenuAction(final @NotNull Project p, final JBList<?> list, final DefaultActionGroup cm) {
        super("Show Context Menu");
        this.p = p;
        this.list = list;
        this.cm = cm;
        this.tree = null;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
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

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
