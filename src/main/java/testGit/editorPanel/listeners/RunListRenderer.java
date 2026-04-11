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
    public Component getListCellRendererComponent(final JList<? extends TestCaseDto> list, final TestCaseDto tc, final int index, final boolean isSelected, final boolean cellHasFocus) {
        int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;

        rendererCard.updateData(globalIndex, tc, ui.isShowGroups(), ui.isShowPriority(), ui.getSelectedDetails());

        boolean isRowHovered = (index == ui.getHoveredIndex());
        String hover = isRowHovered ? ui.getHoveredIconAction() : null;
        rendererCard.setActionsState(isSelected, isRowHovered, hover);

        rendererCard.setBorder(isSelected ?
                JBUI.Borders.customLine(JBColor.blue, 1) :
                JBUI.Borders.empty(1));

        return rendererCard;
    }
}