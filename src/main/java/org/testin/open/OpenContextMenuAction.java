package org.testin.open;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Opens a component's context menu from the keyboard, over whatever is selected
 * in it.
 * <p>
 * A tree and a list answer "where is the selection" in different words, and that
 * is the only difference between them here. So each entry point brings its own
 * answer and everything after it is the same: one component, one menu, one point
 * to show it at. It used to hold a field for each kind and null the other, which
 * made the whole of {@code actionPerformed} a walk through four null checks
 * deciding which half of the class it was in.
 */
public class OpenContextMenuAction extends DumbAwareAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0);

    private final @NotNull JComponent owner;
    private final @NotNull DefaultActionGroup cm;

    /**
     * Where on the owner the menu appears, and empty when nothing is selected -
     * there is no sensible place to put a menu about nothing.
     */
    private final @NotNull Supplier<Optional<Point>> anchor;

    public OpenContextMenuAction(final @NotNull SimpleTree tree, final @NotNull DefaultActionGroup cm) {
        this(tree, cm, () -> selectedRow(tree));
    }

    public OpenContextMenuAction(final @NotNull JBList<?> list, final @NotNull DefaultActionGroup cm) {
        this(list, cm, () -> selectedCell(list));
    }

    private OpenContextMenuAction(final @NotNull JComponent owner, final @NotNull DefaultActionGroup cm,
                                  final @NotNull Supplier<Optional<Point>> anchor) {
        super("Show Context Menu");
        this.owner = owner;
        this.cm = cm;
        this.anchor = anchor;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(SHORTCUT), owner);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        anchor.get().ifPresent(at -> ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, cm)
                .getComponent()
                .show(owner, at.x, at.y));
    }

    /**
     * The middle of the selected row. Swing answers null for the rows of an
     * empty selection, and for the bounds of a row that is not showing.
     */
    private static @NotNull Optional<Point> selectedRow(final @NotNull SimpleTree tree) {
        final int[] rows = tree.getSelectionRows();
        if (rows == null || rows.length == 0) return Optional.empty();

        return Optional.ofNullable(tree.getRowBounds(rows[0]))
                .map(bounds -> new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
    }

    /**
     * A quarter across the selected cell rather than half, so a long test case
     * name is still readable beside the menu.
     */
    private static @NotNull Optional<Point> selectedCell(final @NotNull JBList<?> list) {
        final int index = list.getSelectedIndex();
        if (index == -1) return Optional.empty();

        return Optional.ofNullable(list.getCellBounds(index, index))
                .map(bounds -> new Point(bounds.x + bounds.width / 4, bounds.y + bounds.height / 2));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
