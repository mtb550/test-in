package testGit.editorPanel.listeners;

import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.actions.NavigateToCode;
import testGit.actions.RunTestCase;
import testGit.editorPanel.BaseEditorUI;
import testGit.editorPanel.testRunEditor.RunEditorUI;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static testGit.editorPanel.testRunEditor.RunCard.ACTIONS_TOTAL_WIDTH;

public class HoverListener extends MouseAdapter {

    private final JBList<TestCaseDto> list;
    private final BaseEditorUI ui;

    public HoverListener(JBList<TestCaseDto> list, BaseEditorUI ui) {
        this.list = list;
        this.ui = ui;
    }

    private String getActionAtPoint(int index, int xInCell, int yInCell, Rectangle bounds) {
        if (index == -1) return null;

        if (yInCell <= JBUI.scale(45)) {
            TestCaseDto tc = list.getModel().getElementAt(index);
            int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;
            String titleText = String.format("%d. %s", globalIndex + 1, tc.getTitle());

            Font titleFont = JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f);
            FontMetrics fm = list.getFontMetrics(titleFont);
            int titleWidth = fm.stringWidth(titleText);

            int startX = JBUI.scale(16) + titleWidth + JBUI.scale(10);
            int navStartX = startX - JBUI.scale(6);
            int runStartX = startX + JBUI.scale(22);
            int runEndX = runStartX + JBUI.scale(28);

            if (xInCell >= navStartX && xInCell <= runStartX) return "NAVIGATE";
            if (xInCell > runStartX && xInCell <= runEndX) return "RUN";
        }

        if (ui instanceof RunEditorUI && list.isSelectedIndex(index)) {
            int actionWidth = JBUI.scale(ACTIONS_TOTAL_WIDTH);
            int actionStartX = bounds.width - actionWidth;

            if (xInCell >= actionStartX && xInCell <= bounds.width) {
                int relativeX = xInCell - actionStartX;
                int chunk = actionWidth / 3;

                if (relativeX < chunk) return "PASSED";
                if (relativeX < chunk * 2) return "FAILED";
                return "BLOCKED";
            }
        }

        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index == -1) return;

        Rectangle bounds = list.getCellBounds(index, index);
        String action = getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y, bounds);

        if (action != null) {
            TestCaseDto tc = list.getModel().getElementAt(index);
            if (action.equals("NAVIGATE")) {
                NavigateToCode.execute(tc);

            } else if (action.equals("RUN")) {
                RunTestCase.execute(tc);

            } else {
                System.out.println("Test Case [" + tc.getTitle() + "] updated to: " + action);
            }
            e.consume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        String currentAction = null;

        if (index != -1) {
            Rectangle bounds = list.getCellBounds(index, index);
            currentAction = getActionAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y, bounds);
        }

        if (currentAction != null) {
            list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            list.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        boolean needsRepaint = false;

        if (index != ui.getHoveredIndex()) {
            ui.setHoveredIndex(index);
            needsRepaint = true;
        }

        if (currentAction == null ? ui.getHoveredIconAction() != null : !currentAction.equals(ui.getHoveredIconAction())) {
            ui.setHoveredIconAction(currentAction);
            needsRepaint = true;

            if (currentAction != null) {
                switch (currentAction) {
                    case "NAVIGATE" -> list.setToolTipText("Navigate to Code");
                    case "RUN" -> list.setToolTipText("Run Test Case");
                    case "PASSED" -> list.setToolTipText("Mark as Passed");
                    case "FAILED" -> list.setToolTipText("Mark as Failed");
                    case "BLOCKED" -> list.setToolTipText("Mark as Blocked");
                    default -> list.setToolTipText(null);
                }
            } else {
                list.setToolTipText(null);
            }
        }

        if (needsRepaint) {
            list.repaint();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (ui.getHoveredIndex() != -1 || ui.getHoveredIconAction() != null) {
            ui.setHoveredIndex(-1);
            ui.setHoveredIconAction(null);
            list.repaint();
        }
    }
}