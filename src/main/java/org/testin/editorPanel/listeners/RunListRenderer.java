package org.testin.editorPanel.listeners;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.testin.editorPanel.testRunEditor.RunCard;
import org.testin.editorPanel.testRunEditor.RunEditorUI;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;

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
        final int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;

        TestRunItems runItem;
        runItem = ui.getResultsMap().get(tc.getId());

        rendererCard.updateData(globalIndex, tc, ui.getSelectedDetails(), runItem);

        final boolean isRowHovered = (index == ui.getHoveredIndex());
        final String hover = isRowHovered ? ui.getHoveredIconAction() : null;
        rendererCard.setActionsState(isSelected, isRowHovered, hover);

        rendererCard.setBorder(isSelected ?
                JBUI.Borders.customLine(JBColor.blue, 1) :
                JBUI.Borders.empty(1));

        rendererCard.applyListFont(list.getFont());

        return rendererCard;
    }
}