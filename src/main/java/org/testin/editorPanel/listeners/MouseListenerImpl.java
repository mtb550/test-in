package org.testin.editorPanel.listeners;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.CreateTestCase;
import org.testin.actions.NavigateToCode;
import org.testin.actions.RunTestCase;
import org.testin.editorPanel.EditorContextMenu;
import org.testin.editorPanel.IEditorUI;
import org.testin.pojo.CardHoverAction;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.util.logger.Log;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MouseListenerImpl extends MouseAdapter {
    private final Project project;
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;
    private final EditorContextMenu editorCm;
    private final DefaultActionGroup emptyMenu;
    private final Path path;
    private final IEditorUI ui;

    public MouseListenerImpl(final @NotNull Project project, final IEditorUI ui, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model, final DirectoryDto dir, final EditorContextMenu editorCm) {
        this.project = project;
        this.ui = ui;
        this.list = list;
        this.path = dir.getPath();
        this.model = model;
        this.editorCm = editorCm;
        this.emptyMenu = new DefaultActionGroup();
        this.emptyMenu.add(new CreateTestCase(ui, dir, list, model));
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        final boolean isClickOnItem = index >= 0 && list.getCellBounds(index, index).contains(e.getPoint());

        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            if (isClickOnItem)
                Optional.ofNullable(model.getElementAt(index)).ifPresent(selected -> ViewToolWindowFactory.showPanel(project, List.of(selected), path));

            return;
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            final ActionManager actionManager = ActionManager.getInstance();
            final String place = ActionPlaces.TOOLWINDOW_POPUP;

            if (isClickOnItem) {
                if (!list.isSelectedIndex(index)) {
                    list.setSelectedIndex(index);
                }
                actionManager.createActionPopupMenu(place, editorCm).getComponent().show(e.getComponent(), e.getX(), e.getY());

            } else {
                list.clearSelection();
                actionManager.createActionPopupMenu(place, emptyMenu).getComponent().show(e.getComponent(), e.getX(), e.getY());
            }
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

            if (action == CardHoverAction.NAVIGATE) {
                Log.trace("navigate action, tc: " + tc.getDescription());
                new NavigateToCode(list).execute(project, tc);

            } else if (action == CardHoverAction.RUN) {
                Log.trace("run action, tc: " + tc.getDescription());
                new RunTestCase(list).execute(project, tc);
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

        if (index != ui.getHoveredIndex()) {
            ui.setHoveredIndex(index);
            needsRepaint = true;
        }

        final String actionName = currentAction != null ? currentAction.name() : null;

        if (actionName == null ? ui.getHoveredIconAction() != null : !actionName.equals(ui.getHoveredIconAction())) {
            ui.setHoveredIconAction(actionName);
            needsRepaint = true;

            list.setToolTipText(Optional.ofNullable(currentAction).map(CardHoverAction::getTooltip).orElse(null));
        }

        if (needsRepaint)
            list.repaint();

    }

    @Override
    public void mouseExited(final MouseEvent e) {
        if (ui.getHoveredIndex() != -1 || ui.getHoveredIconAction() != null) {
            ui.setHoveredIndex(-1);
            ui.setHoveredIconAction(null);
            list.setToolTipText(null);
            list.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(final MouseWheelEvent e) {
        if (e.isControlDown() || e.isMetaDown())
            return;

        JScrollPane scrollPane = getScrollPane(e.getComponent());

        if (scrollPane != null && e.getComponent() != scrollPane) {
            MouseWheelEvent clonedEvent = (MouseWheelEvent) SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane);
            scrollPane.dispatchEvent(clonedEvent);
            e.consume();
        }
    }

    private CardHoverAction getActionAtPoint(final int index, final int xInCell, final int yInCell) {
        if (index == -1) return null;

        float baseSize = list.getFont().getSize2D();

        final Font titleFont = list.getFont().deriveFont(Font.BOLD, baseSize + 2.0f);
        final FontMetrics fm = list.getFontMetrics(titleFont);

        int dynamicYBound = fm.getHeight() + JBUI.scale(20);

        if (yInCell <= dynamicYBound) {
            final TestCaseDto tc = list.getModel().getElementAt(index);
            final int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;
            final String titleText = String.format(Locale.ENGLISH, "%d. %s", globalIndex + 1, tc.getDescription());

            final int titleWidth = fm.stringWidth(titleText);

            final int startX = JBUI.scale(16) + titleWidth + JBUI.scale(10);
            final int navStartX = startX - JBUI.scale(6);
            final int runStartX = startX + JBUI.scale(22);
            final int runEndX = runStartX + JBUI.scale(28);

            if (xInCell >= navStartX && xInCell <= runStartX) return CardHoverAction.NAVIGATE;
            if (xInCell > runStartX && xInCell <= runEndX) return CardHoverAction.RUN;
        }

        return null;
    }

    private JScrollPane getScrollPane(final Component component) {
        Component current = component;
        while (current != null) {
            if (current instanceof JScrollPane)
                return (JScrollPane) current;

            current = current.getParent();
        }

        return null;
    }
}