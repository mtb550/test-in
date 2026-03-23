package testGit.editorPanel.listeners;

import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import testGit.actions.NavigateToCode;
import testGit.actions.RunTestCase;
import testGit.editorPanel.BaseEditorUI;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ActionInteractionListener extends MouseAdapter {

    private final JBList<TestCaseDto> list;
    private final BaseEditorUI ui;

    public ActionInteractionListener(JBList<TestCaseDto> list, BaseEditorUI ui) {
        this.list = list;
        this.ui = ui;
    }

    private String getIconAtPoint(int index, int xInCell, int yInCell) {
        if (index == -1 || !list.isSelectedIndex(index)) return null;

        if (yInCell > 45) return null;

        TestCaseDto tc = list.getModel().getElementAt(index);
        int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;
        String titleText = (globalIndex + 1) + ". " + tc.getTitle();

        Font titleFont = JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f);
        FontMetrics fm = list.getFontMetrics(titleFont);
        int titleWidth = fm.stringWidth(titleText);

        int startX = 16 + titleWidth + 10;
        int navigateStartX = startX;
        int navigateEndX = navigateStartX + 28;
        int runStartX = navigateEndX + 8;
        int runEndX = runStartX + 28;

        if (xInCell >= navigateStartX && xInCell <= navigateEndX) return "NAVIGATE";
        if (xInCell >= runStartX && xInCell <= runEndX) return "RUN";

        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index == -1 || !list.isSelectedIndex(index)) return;

        Rectangle bounds = list.getCellBounds(index, index);
        String action = getIconAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y);

        if (action != null) {
            TestCaseDto tc = list.getModel().getElementAt(index);
            if (action.equals("NAVIGATE")) NavigateToCode.execute(tc);
            else if (action.equals("RUN")) RunTestCase.execute(tc);
            e.consume();
        }
    }
}