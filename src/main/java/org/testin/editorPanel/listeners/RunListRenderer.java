package org.testin.editorPanel.listeners;

import org.testin.editorPanel.runEditor.RunCard;
import org.testin.editorPanel.runEditor.RunEditorUI;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;

public class RunListRenderer extends AbstractListRenderer<RunEditorUI> {
    private final RunCard card;

    public RunListRenderer(final RunEditorUI ui) {
        super(ui);
        this.card = new RunCard(ui.getProject());
    }

    @Override
    protected JComponent bindDataAndGetCard(JList<? extends TestCaseDto> list, TestCaseDto tc, int globalIndex, boolean isSelected, boolean isRowHovered, String hover) {
        final TestRunItems runItem = ui.getResultsMap().get(tc.getId());

        card.updateData(globalIndex, ui.getSelectedDetails(), runItem);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}