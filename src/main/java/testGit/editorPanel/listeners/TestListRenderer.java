package testGit.editorPanel.listeners;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import testGit.editorPanel.testCaseEditor.TestCard;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public class TestListRenderer implements ListCellRenderer<TestCaseDto> {
    private final TestCard rendererCard = new TestCard();
    private final TestEditorUI ui;

    public TestListRenderer(final TestEditorUI ui) {
        this.ui = ui;
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends TestCaseDto> list, final TestCaseDto tc, final int index, final boolean isSelected, final boolean cellHasFocus) {

        int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;
        boolean isUnsorted = ui.getUnsortedIds().contains(tc.getId());

        rendererCard.updateData(globalIndex, tc, ui.isShowGroups(), ui.isShowPriority(), ui.getSelectedDetails(), isUnsorted);

        // 🌟 1. الأيقونات تظهر فقط إذا كان الصف محدداً!
        rendererCard.setActionsState(isSelected, isSelected ? ui.getHoveredIconAction() : null);

        rendererCard.setBorder(isSelected ?
                JBUI.Borders.customLine(JBColor.blue, 1) :
                JBUI.Borders.empty(1));

        return rendererCard;
    }
}