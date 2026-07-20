package org.testin.editorPanel.listeners;

import org.testin.editorPanel.runEditor.RunCard;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;

public class RunListRenderer extends AbstractListRenderer<RunEditor> {
    private final RunCard card;

    public RunListRenderer(final RunEditor editor) {
        super(editor);
        this.card = new RunCard(editor.getProject());
    }

    @Override
    protected JComponent bindDataAndGetCard(JList<? extends TestCaseDto> list, TestCaseDto tc, int globalIndex, boolean isSelected, boolean isRowHovered, String hover) {
        final TestRunItems runItem = editor.getResultsMap().get(tc.getId());

        card.updateData(globalIndex, editor.getSelectedDetails(), runItem);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}