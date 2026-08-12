package org.testin.editorPanel.listeners;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.AbstractEditorContextMenu;
import org.testin.editorPanel.BaseCard;
import org.testin.editorPanel.IEditor;
import org.testin.enums.CardHoverAction;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.run.RunTestCaseAction;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MouseListenerImpl extends MouseAdapter {
    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull CollectionListModel<TestCaseDto> model;
    private final @NotNull AbstractEditorContextMenu cm;
    private final @NotNull ArrayList<String> path;
    private final @NotNull IEditor editor;

    public MouseListenerImpl(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model, final @NotNull DirectoryDto dir, final @NotNull AbstractEditorContextMenu cm) {
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
            if (isClickOnItem)
                Optional.ofNullable(model.getElementAt(index)).ifPresent(selected -> ViewToolWindowFactory.showPanel(p, List.of(selected), path));

            return;
        }

        if (!isClickOnItem) {
            list.getSelectionModel().clearSelection();
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            if (isClickOnItem && !list.getSelectionModel().isSelectedIndex(index))
                list.getSelectionModel().setSelectionInterval(index, index);

            final ActionManager actionManager = ActionManager.getInstance();
            final String place = ActionPlaces.TOOLWINDOW_POPUP;
            actionManager.createActionPopupMenu(place, cm).getComponent().show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;

        final int index = list.locationToIndex(e.getPoint());
        if (index == -1) return;

        final Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(e.getPoint())) return;

        final CardHoverAction action = getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y);

        if (action != null) {
            final TestCaseDto tc = list.getModel().getElementAt(index);

            if (action == CardHoverAction.NAVIGATE_TO_TEST_METHOD) {
                Logger.trace("org.testin.navigate action, tc: " + tc.getDescription());
                new NavigateToCodeAction(p, list).execute(p, tc);

            } else if (action == CardHoverAction.RUN_TEST_CASE) {
                Logger.trace("run action, tc: " + tc.getDescription());
                new RunTestCaseAction(p, list).execute(tc);
            }

            e.consume();
        }
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        CardHoverAction currentAction = null;

        if (index != -1) {
            final Rectangle bounds = list.getCellBounds(index, index);

            if (bounds.contains(e.getPoint())) {
                currentAction = getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y);
            }
        }

        list.setCursor(Cursor.getPredefinedCursor(currentAction != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        boolean needsRepaint = false;

        if (index != editor.getHoveredIndex()) {
            editor.setHoveredIndex(index);
            needsRepaint = true;
        }

        final String actionName = currentAction != null ? currentAction.name() : null;

        if (actionName == null ? editor.getHoveredIconAction() != null : !actionName.equals(editor.getHoveredIconAction())) {
            editor.setHoveredIconAction(actionName);
            needsRepaint = true;

            list.setToolTipText(Optional.ofNullable(currentAction).map(CardHoverAction::getHintText).orElse(null));
        }

        if (needsRepaint)
            list.repaint();
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        if (editor.getHoveredIndex() != -1 || editor.getHoveredIconAction() != null) {
            editor.setHoveredIndex(-1);
            editor.setHoveredIconAction(null);
            list.setToolTipText(null);
            list.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(final MouseWheelEvent e) {
        if (e.isControlDown() || e.isMetaDown())
            return;

        final JBScrollPane scrollPane = getScrollPane(e.getComponent());

        if (scrollPane != null && e.getComponent() != scrollPane) {
            final MouseWheelEvent clonedEvent = (MouseWheelEvent) SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane);
            scrollPane.dispatchEvent(clonedEvent);
            e.consume();
        }
    }

    private @Nullable CardHoverAction getActionAtPoint(final int index, final int xInCell, final int yInCell) {
        if (index == -1) return null;

        final float baseSize = list.getFont().getSize2D();

        // Must match the painted title font, otherwise the hover hit targets drift
        // away from the drawn icons as the title grows.
        final Font titleFont = list.getFont().deriveFont(Font.BOLD, baseSize + BaseCard.TITLE_FONT_DELTA);
        final FontMetrics fm = list.getFontMetrics(titleFont);

        final int dynamicYBound = fm.getHeight() + JBUI.scale(20);

        if (yInCell <= dynamicYBound) {
            final TestCaseDto tc = list.getModel().getElementAt(index);
            final int globalIndex = ((editor.getCurrentPage() - 1) * editor.getPageSize()) + index;
            final String titleText = String.format(Locale.ENGLISH, "%d. %s", globalIndex + 1, tc.getDescription());

            final int titleWidth = fm.stringWidth(titleText);

            final int startX = JBUI.scale(16) + titleWidth + JBUI.scale(10);
            final int navStartX = startX - JBUI.scale(6);
            final int runStartX = startX + JBUI.scale(22);
            final int runEndX = runStartX + JBUI.scale(28);

            if (xInCell >= navStartX && xInCell <= runStartX) return CardHoverAction.NAVIGATE_TO_TEST_METHOD;
            if (xInCell > runStartX && xInCell <= runEndX) return CardHoverAction.RUN_TEST_CASE;
        }

        return null;
    }

    private @Nullable JBScrollPane getScrollPane(final @Nullable Component component) {
        Component current = component;
        while (current != null) {
            if (current instanceof JBScrollPane)
                return (JBScrollPane) current;

            current = current.getParent();
        }

        return null;
    }
}