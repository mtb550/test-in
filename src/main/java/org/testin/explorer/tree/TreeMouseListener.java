package org.testin.explorer.tree;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.open.OpenAction;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;

@AllArgsConstructor
public class TreeMouseListener extends PopupHandler {
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull TreeContextMenu treeContextMenu;

    @Override
    public void invokePopup(final @NotNull Component comp, final int x, final int y) {
        final TreePath selPath = rowPathAt(x, y);

        if (selPath != null && TreeValueUtil.directoryOf(selPath.getLastPathComponent()) != null) {

            if (!tree.getSelectionModel().isPathSelected(selPath)) {
                tree.setSelectionPath(selPath);
            }

            final ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, treeContextMenu);
            popupMenu.getComponent().show(comp, x, y);
        }
    }

    @Override
    public void mouseClicked(final @NotNull MouseEvent e) {
        final TreePath selPath = rowPathAt(e.getX(), e.getY());

        if (selPath == null || TreeValueUtil.directoryOf(selPath.getLastPathComponent()) == null)
            return;

        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            new OpenAction(p, tree).execute(p);
            e.consume();
        }
    }

    /**
     * The whole row responds, not only the label: the wide selection paints
     * the full width, so clicks in the indentation or right of the text must
     * hit the same node. Matches the row by Y alone; below the last row is
     * still a miss.
     */
    private @Nullable TreePath rowPathAt(final int x, final int y) {
        final int row = tree.getClosestRowForLocation(x, y);
        if (row < 0) return null;

        final Rectangle bounds = tree.getRowBounds(row);
        if (bounds == null || y < bounds.y || y >= bounds.y + bounds.height) return null;

        return tree.getPathForRow(row);
    }
}
