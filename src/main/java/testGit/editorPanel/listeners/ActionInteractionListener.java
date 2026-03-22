package testGit.editorPanel.listeners;

import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import testGit.actions.NavigateToCode;
import testGit.actions.RunTestCase;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ActionInteractionListener extends MouseAdapter {

    private final JBList<TestCaseDto> list;
    private final TestEditorUI ui;

    public ActionInteractionListener(JBList<TestCaseDto> list, TestEditorUI ui) {
        this.list = list;
        this.ui = ui;
    }

    // حساب المنطقة بناءً على طول النص (تعمل فقط إذا كان الصف محدداً)
    private String getIconAtPoint(int index, int xInCell, int yInCell) {
        if (index == -1 || !list.isSelectedIndex(index)) return null;

        // الأيقونات توجد في الجزء العلوي، نتجاهل النقرات في الأسفل
        if (yInCell > 45) return null;

        TestCaseDto tc = list.getModel().getElementAt(index);
        int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;
        String titleText = (globalIndex + 1) + ". " + tc.getTitle();

        Font titleFont = JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f);
        FontMetrics fm = list.getFontMetrics(titleFont);
        int titleWidth = fm.stringWidth(titleText);

        // 16 هامش + عرض النص + 10 مسافة
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
    public void mouseMoved(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index == -1) return;

        Rectangle bounds = list.getCellBounds(index, index);
        String currentIcon = getIconAtPoint(index, e.getX() - bounds.x, e.getY() - bounds.y);

        // تغيير شكل الماوس لليد إذا كنا فوق الأيقونة
        list.setCursor(currentIcon != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        // طلب إعادة رسم لتلوين الخلفية الشفافة
        if (currentIcon == null ? ui.getHoveredIconAction() != null : !currentIcon.equals(ui.getHoveredIconAction())) {
            ui.setHoveredIconAction(currentIcon);
            list.repaint();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (ui.getHoveredIconAction() != null) {
            ui.setHoveredIconAction(null);
            list.repaint();
        }
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
            e.consume(); // منع الـ JList من إضاعة التحديد
        }
    }
}