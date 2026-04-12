package testGit.editorPanel.listeners;

import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.actions.NavigateToCode;
import testGit.actions.RunTestCase;
import testGit.editorPanel.BaseEditorUI;
import testGit.editorPanel.testRunEditor.RunEditorUI;
import testGit.pojo.CardHoverAction;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

import static testGit.editorPanel.testRunEditor.RunCard.ACTIONS_TOTAL_WIDTH;

public class HoverListener extends MouseAdapter {

    private final JBList<TestCaseDto> list;
    private final BaseEditorUI ui;

    public HoverListener(final JBList<TestCaseDto> list, final BaseEditorUI ui) {
        this.list = list;
        this.ui = ui;
    }

    private CardHoverAction getActionAtPoint(final int index, final int xInCell, final int yInCell, final Rectangle bounds) {
        if (index == -1) return null;

        if (yInCell <= JBUI.scale(45)) {
            final TestCaseDto tc = list.getModel().getElementAt(index);
            final int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;
            final String titleText = String.format("%d. %s", globalIndex + 1, tc.getTitle());

            final Font titleFont = JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f);
            final FontMetrics fm = list.getFontMetrics(titleFont);
            final int titleWidth = fm.stringWidth(titleText);

            final int startX = JBUI.scale(16) + titleWidth + JBUI.scale(10);
            final int navStartX = startX - JBUI.scale(6);
            final int runStartX = startX + JBUI.scale(22);
            final int runEndX = runStartX + JBUI.scale(28);

            if (xInCell >= navStartX && xInCell <= runStartX) return CardHoverAction.NAVIGATE;
            if (xInCell > runStartX && xInCell <= runEndX) return CardHoverAction.RUN;
        }

        if (ui instanceof RunEditorUI && list.isSelectedIndex(index)) {
            final int actionWidth = JBUI.scale(ACTIONS_TOTAL_WIDTH);
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
    public void mouseClicked(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        if (index == -1) return;

        final Rectangle bounds = list.getCellBounds(index, index);
        final CardHoverAction action = getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y, bounds);

        if (action != null) {
            final TestCaseDto tc = list.getModel().getElementAt(index);

            if (action == CardHoverAction.NAVIGATE) {
                NavigateToCode.execute(tc);
            } else if (action == CardHoverAction.RUN) {
                RunTestCase.execute(tc);
            } else {
                System.out.println("Test Case [" + tc.getTitle() + "] updated to: " + action.name());
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
            list.repaint();
        }
    }
}