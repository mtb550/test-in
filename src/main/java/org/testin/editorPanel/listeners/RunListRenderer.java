package org.testin.editorPanel.listeners;

import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.runEditor.RunCard;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;

public class RunListRenderer extends AbstractListRenderer<RunEditor> {
    private final RunCard card;

    public RunListRenderer(final RunEditor editor) {
        super(editor);
        this.card = new RunCard(editor.getProject());
    }

    @Override
    protected JComponent bindDataAndGetCard(@NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int globalIndex, final boolean isSelected, final boolean isRowHovered, final String hover) {
        final TestRunItems runItem = editor.getResultsMap().get(tc.getId());

        card.updateData(globalIndex, editor.getSelectedDetails(), runItem);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}