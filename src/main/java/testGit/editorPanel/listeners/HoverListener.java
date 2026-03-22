//package testGit.editorPanel.listeners;
//
//import com.intellij.ui.components.JBList;
//import testGit.actions.NavigateToCode;
//import testGit.actions.RunTestCase;
//import testGit.editorPanel.testCaseEditor.TestEditorUI;
//import testGit.pojo.dto.TestCaseDto;
//
//import java.awt.*;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//
//public class HoverListener extends MouseAdapter {
//
//    private final JBList<TestCaseDto> list;
//    private final TestEditorUI ui;
//
//    public HoverListener(JBList<TestCaseDto> list, TestEditorUI ui) {
//        this.list = list;
//        this.ui = ui;
//    }
//
//    @Override
//    public void mouseMoved(MouseEvent e) {
//        int index = list.locationToIndex(e.getPoint());
//        String currentIcon = null;
//
//        if (index != -1) {
//            Rectangle bounds = list.getCellBounds(index, index);
//            int xInCell = e.getX() - bounds.x;
//
//            // 🌟 الحساب الدقيق:
//            // الهامش الأيمن = 16، عرض الأيقونة تقريباً = 28
//            int runStartX = bounds.width - 16 - 28;
//            int navigateStartX = runStartX - 28 - 10; // 10 هي المسافة بين الأيقونتين
//
//            if (xInCell >= runStartX && xInCell <= bounds.width - 16) {
//                currentIcon = "RUN";
//                list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//            } else if (xInCell >= navigateStartX && xInCell < runStartX) {
//                currentIcon = "NAVIGATE";
//                list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//            } else {
//                list.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
//            }
//        }
//
//        boolean needsRepaint = false;
//        if (index != ui.getHoveredIndex()) {
//            ui.setHoveredIndex(index);
//            needsRepaint = true;
//        }
//
//        // إذا تغيرت الأيقونة التي نقف عليها، اطلب إعادة الرسم لتلوينها
//        if (currentIcon == null ? ui.getHoveredIconAction() != null : !currentIcon.equals(ui.getHoveredIconAction())) {
//            ui.setHoveredIconAction(currentIcon);
//            needsRepaint = true;
//        }
//
//        if (needsRepaint) list.repaint();
//    }
//
//    @Override
//    public void mouseExited(MouseEvent e) {
//        if (ui.getHoveredIndex() != -1 || ui.getHoveredIconAction() != null) {
//            ui.setHoveredIndex(-1);
//            ui.setHoveredIconAction(null);
//            list.repaint();
//        }
//    }
//
//    @Override
//    public void mouseClicked(MouseEvent e) {
//        int index = list.locationToIndex(e.getPoint());
//        if (index == -1) return;
//
//        // استخدمنا المتغير الجاهز الذي حسبناه في mouseMoved لضمان تطابق النقر مع التلوين!
//        String action = ui.getHoveredIconAction();
//
//        if (action != null) {
//            TestCaseDto tc = list.getModel().getElementAt(index);
//            if (action.equals("NAVIGATE")) {
//                NavigateToCode.execute(tc);
//            } else if (action.equals("RUN")) {
//                RunTestCase.execute(tc);
//            }
//            e.consume();
//        }
//    }
//}