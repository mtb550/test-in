package testGit.editorPanel.listeners;

import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.actions.NavigateToCode;
import testGit.actions.RunTestCase;
import testGit.editorPanel.BaseEditorUI;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class ActionInteractionListener extends MouseAdapter {

    private final JBList<TestCaseDto> list;
    private final BaseEditorUI ui;

    public ActionInteractionListener(JBList<TestCaseDto> list, BaseEditorUI ui) {
        this.list = list;
        this.ui = ui;
    }

    private String getIconAtPoint(int index, int xInCell, int yInCell) {
        // الأيقونات لا تظهر إلا إذا كان الصف محدداً
        if (index == -1) return null;

        // منطقة النقر العمودية (أعلى 45 بكسل لضمان سهولة التقاط الماوس)
        if (yInCell > JBUI.scale(45)) return null;

        TestCaseDto tc = list.getModel().getElementAt(index);
        int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;
        String titleText = String.format("%d. %s", globalIndex + 1, tc.getTitle());

        Font titleFont = JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f);
        FontMetrics fm = list.getFontMetrics(titleFont);
        int titleWidth = fm.stringWidth(titleText);

        // منطقة النقر الأفقية باستخدام JBUI.scale لكي تتطابق إحداثيات الماوس مع الرسام تماماً
        int startX = JBUI.scale(16) + titleWidth + JBUI.scale(10);

        // إعطاء مساحة (Padding) وهمية لسهولة النقر والتمرير
        int navStartX = startX - JBUI.scale(6);
        int navEndX = startX + JBUI.scale(22);

        int runStartX = navEndX;
        int runEndX = runStartX + JBUI.scale(28);

        if (xInCell >= navStartX && xInCell <= navEndX) return "NAVIGATE";
        if (xInCell > runStartX && xInCell <= runEndX) return "RUN";

        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index == -1) return;

        Rectangle bounds = list.getCellBounds(index, index);
        String action = getIconAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y);

        if (action != null) {
            TestCaseDto tc = list.getModel().getElementAt(index);
            if (action.equals("NAVIGATE")) NavigateToCode.execute(tc);
            else if (action.equals("RUN")) RunTestCase.execute(tc);
            e.consume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index == -1) {
            clearHover();
            return;
        }

        Rectangle bounds = list.getCellBounds(index, index);
        String currentIcon = getIconAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y);

        list.setCursor(currentIcon != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        // التحقق الصحيح وتحديث الواجهة
        if (!Objects.equals(currentIcon, ui.getHoveredIconAction()) || ui.getHoveredIndex() != index) {
            ui.setHoveredIndex(index);
            ui.setHoveredIconAction(currentIcon);
            list.repaint();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        clearHover();
    }

    private void clearHover() {
        if (ui.getHoveredIconAction() != null) {
            ui.setHoveredIconAction(null);
            ui.setHoveredIndex(-1);
            list.repaint();
        }
    }
}