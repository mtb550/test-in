package testGit.editorPanel.listeners;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import testGit.editorPanel.testRunEditor.RunCard;
import testGit.editorPanel.testRunEditor.RunEditorUI;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public class RunListRenderer implements ListCellRenderer<TestCaseDto> {
    private final RunCard rendererCard = new RunCard();
    private final RunEditorUI ui;

    public RunListRenderer(final RunEditorUI ui) {
        this.ui = ui;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TestCaseDto> list, TestCaseDto tc, int index, boolean isSelected, boolean cellHasFocus) {
        int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;

        rendererCard.updateData(globalIndex, tc, ui.isShowGroups(), ui.isShowPriority(), ui.getSelectedDetails());
        rendererCard.setActionsState(isSelected); // 🌟 إزالة المعامل الثاني

        rendererCard.setBorder(isSelected ?
                JBUI.Borders.customLine(JBColor.blue, 1) :
                JBUI.Borders.empty(1));

        return rendererCard;
    }
}