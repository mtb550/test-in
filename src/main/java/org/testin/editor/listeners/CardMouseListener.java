package org.testin.editor.listeners;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.*;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CardMouseListener extends MouseAdapter {
    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull CollectionListModel<TestCaseDto> model;
    private final @NotNull AbstractEditorContextMenu cm;
    private final @NotNull ArrayList<String> path;
    private final @NotNull TestinEditor editor;

    public CardMouseListener(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model, final @NotNull DirectoryDto dir, final @NotNull AbstractEditorContextMenu cm) {
        this.p = p;
        this.editor = editor;
        this.list = list;
        this.path = dir.getPath2();
        this.model = model;
        this.cm = cm;
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        final boolean isClickOnItem = index >= 0 && list.getCellBounds(index, index).contains(e.getPoint());

        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            // Focused, not just shown: the details tab carries the F2 binding
            // itself, so landing the focus there is what lets a tester open the
            // update menu straight after the double-click. The same call the
            // View Details actions make - the double-click was the one way in
            // that showed the panel and left the focus behind.
            if (isClickOnItem)
                Optional.ofNullable(model.getElementAt(index)).ifPresent(selected -> ViewToolWindowFactory.showPanel(p, List.of(selected), path, ViewPanel::focusDetailsTab));

            return;
        }

        if (!isClickOnItem) {
            list.getSelectionModel().clearSelection();
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            if (isClickOnItem && !list.getSelectionModel().isSelectedIndex(index))
                list.getSelectionModel().setSelectionInterval(index, index);

            final @NotNull ActionManager actionManager = ActionManager.getInstance();
            final @NotNull String place = ActionPlaces.TOOLWINDOW_POPUP;
            actionManager.createActionPopupMenu(place, cm).getComponent().show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;

        final int index = list.locationToIndex(e.getPoint());
        if (index == -1) return;

        final @NotNull Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(e.getPoint())) return;

        getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y).ifPresent(action -> {
            final @NotNull TestCaseDto tc = list.getModel().getElementAt(index);

            Logger.trace(action.getTooltip() + ", tc: " + tc.getDescription());
            // Which editor the tester clicked in. The reports that follow name
            // only the case, and a case can be in several open runs.
            editor.launching(tc.getId());

            action.execute(p, tc);

            e.consume();
        });
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        final @NotNull Optional<CardHoverAction> currentAction = actionUnder(e, index);

        list.setCursor(Cursor.getPredefinedCursor(currentAction.isPresent() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        boolean needsRepaint = false;

        if (index != editor.getHoveredIndex()) {
            editor.setHoveredIndex(index);
            needsRepaint = true;
        }

        final @NotNull String actionName = currentAction.map(Enum::name).orElse("");

        if (!actionName.equals(editor.getHoveredIconAction())) {
            editor.setHoveredIconAction(actionName);
            needsRepaint = true;

            // Swing's own contract: a null tooltip is no tooltip, and an empty
            // one is a small empty box that follows the pointer.
            list.setToolTipText(currentAction.map(CardHoverAction::getHintText).orElse(null));
        }

        if (needsRepaint)
            list.repaint();
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        if (editor.getHoveredIndex() != -1 || !editor.getHoveredIconAction().isEmpty()) {
            editor.setHoveredIndex(-1);
            editor.setHoveredIconAction("");
            list.setToolTipText(null);
            list.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(final MouseWheelEvent e) {
        Shared.forwardWheelToScrollPane(e);
    }

    /**
     * The action under the pointer, when the pointer is inside a row at all.
     */
    private @NotNull Optional<CardHoverAction> actionUnder(final @NotNull MouseEvent e, final int index) {
        if (index == -1) return Optional.empty();

        final @NotNull Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(e.getPoint())) return Optional.empty();

        return getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y);
    }

    private @NotNull Optional<CardHoverAction> getActionAtPoint(final int index, final int xInCell, final int yInCell) {
        if (index == -1) return Optional.empty();

        // Must match the font the card paints the title in, or the width is
        // measured against the wrong glyphs and every target shifts.
        final float baseSize = list.getFont().getSize2D();
        final @NotNull Font titleFont = list.getFont().deriveFont(Font.BOLD, baseSize + BaseCard.TITLE_FONT_DELTA);

        // The title is asked of the editor, which owns what it reads, and where
        // the icons sit is asked of Shared, which paints them. Neither is worked
        // out here: both used to be, and both drifted.
        final @NotNull TestCaseDto tc = list.getModel().getElementAt(index);
        final @NotNull String title = editor.cardTitle(tc);

        // Capped at the title column exactly as the card caps what it paints, so
        // a title long enough to wrap keeps the clickable band under the icons
        // rather than out past the edge of the card.
        final int titleWidth = Math.min(list.getFontMetrics(titleFont).stringWidth(title), Shared.titleColumnWidth(list.getWidth()));

        // The card draws the run button or the stop button by the same rule, so
        // the pointer is over whichever one this case is offering.
        return Shared.descriptionActionIcons(titleWidth).at(xInCell, yInCell, CardHoverAction.runSlot(p, tc));
    }

}
