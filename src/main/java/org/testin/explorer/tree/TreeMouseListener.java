package org.testin.explorer.tree;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.open.OpenAction;

import javax.swing.*;
import java.util.Optional;
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
        nodeAt(x, y).ifPresent(selPath -> {
            if (!tree.getSelectionModel().isPathSelected(selPath)) {
                tree.setSelectionPath(selPath);
            }

            final @NotNull ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, treeContextMenu);
            popupMenu.getComponent().show(comp, x, y);
        });
    }

    @Override
    public void mouseClicked(final @NotNull MouseEvent e) {
        if (nodeAt(e.getX(), e.getY()).isEmpty()) return;

        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            new OpenAction(p, tree).execute(p);
            e.consume();
        }
    }

    /**
     * The node under the pointer, and empty when there is none there.
     * <p>
     * The whole row responds, not only the label: the wide selection paints the
     * full width, so clicks in the indentation or right of the text hit the same
     * node. Matched by Y alone, and below the last row is still a miss.
     * <p>
     * A path with nothing behind it is a miss too. Both callers wanted a node
     * rather than a path, and each used to ask for the path and then ask again
     * whether it had one - the same question in two places (#71).
     */
    private @NotNull Optional<TreePath> nodeAt(final int x, final int y) {
        final int row = tree.getClosestRowForLocation(x, y);
        if (row < 0) return Optional.empty();

        return Optional.ofNullable(tree.getRowBounds(row))
                .filter(bounds -> y >= bounds.y && y < bounds.y + bounds.height)
                .map(bounds -> tree.getPathForRow(row))
                .filter(path -> TreeValueUtil.directoryAt(path).isPresent());
    }
}
