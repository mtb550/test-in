package org.testin.editor.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.run.RunCard;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;

public class RunListRenderer extends AbstractListRenderer<RunEditor> {
    private final @NotNull RunCard card;

    public RunListRenderer(final @NotNull Project p, final @NotNull RunEditor editor) {
        super(editor);
        this.card = new RunCard(p);
    }

    @Override
    protected @NotNull JComponent bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int globalIndex, final boolean isSelected, final boolean isRowHovered, final @Nullable String hover) {
        TestRunItems runItem = editor.getResultsMap().get(tc.getId());

        // The results map can be transiently empty during a refresh while the list still repaints;
        // render a pending placeholder instead of crashing inside the cell renderer.
        if (runItem == null) {
            runItem = TestRunItems.builder().id(tc.getId()).tc(tc).build();
        }

        card.updateData(globalIndex, editor.getSelectedDetails(), runItem);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}