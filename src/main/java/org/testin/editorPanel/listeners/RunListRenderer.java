package org.testin.editorPanel.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.runEditor.RunCard;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;

public class RunListRenderer extends AbstractListRenderer<RunEditor> {
    private final @NotNull Project p;
    private final @NotNull RunCard card;

    public RunListRenderer(final @NotNull Project p, final @NotNull RunEditor editor) {
        super(editor);
        this.p = p;
        this.card = new RunCard(p);
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