package org.testin.editorPanel.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.NavigateToCode;
import org.testin.actions.RunTestCase;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.testRunEditor.RunEditorUI;
import org.testin.pojo.CardHoverAction;
import org.testin.pojo.Config;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.logger.Log;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.Optional;

import static org.testin.editorPanel.testRunEditor.RunCard.ACTIONS_TOTAL_WIDTH;

public class HoverListener extends MouseAdapter {

    private final Project project;
    private final JBList<TestCaseDto> list;
    private final IEditorUI ui;

    public HoverListener(final @NotNull Project project, final JBList<TestCaseDto> list, final IEditorUI ui) {
        this.project = project;
        this.list = list;
        this.ui = ui;
    }

    private CardHoverAction getActionAtPoint(final int index, final int xInCell, final int yInCell, final Rectangle bounds) {
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

        if (ui instanceof RunEditorUI && list.isSelectedIndex(index)) {
            float fontScaleMultiplier = baseSize / 15.0f;
            final int actionWidth = (int) (JBUI.scale(ACTIONS_TOTAL_WIDTH) * fontScaleMultiplier);
            final int actionStartX = bounds.width - actionWidth;

            if (xInCell >= actionStartX && xInCell <= bounds.width) {
                final int relativeX = xInCell - actionStartX;
                final int chunk = actionWidth / 3;

                if (relativeX < chunk) return CardHoverAction.PASSED;
                if (relativeX < chunk * 2) return CardHoverAction.FAILED;
                return CardHoverAction.BLOCKED;
            }
        }

        return null;
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        if (index == -1) return;

        final Rectangle bounds = list.getCellBounds(index, index);

        if (!bounds.contains(e.getPoint())) return;

        final CardHoverAction action = getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y, bounds);

        if (action != null) {
            final TestCaseDto tc = list.getModel().getElementAt(index);

            if (action == CardHoverAction.NAVIGATE) {
                new NavigateToCode(list).execute(project, tc);
            } else if (action == CardHoverAction.RUN) {
                new RunTestCase(list).execute(project, tc);
            } else {
                try {
                    TestStatus status = TestStatus.valueOf(action.name());
                    if (ui instanceof RunEditorUI runUi) {
                        int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;

                        if (globalIndex == runUi.getCurrentlyExecutingIndex()) {
                            runUi.updateStatusAndNext(status);
                        } else {
                            runUi.handleManualStatusUpdate(tc, status);
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    Log.info("Unknown action: " + action.name());
                }
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

            if (bounds.contains(e.getPoint()))
                currentAction = getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y, bounds);
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

        if (needsRepaint) {
            list.repaint();
        }
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
}