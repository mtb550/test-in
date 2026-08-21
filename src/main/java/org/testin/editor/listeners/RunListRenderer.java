package org.testin.editor.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
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
    protected @NotNull RunCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int globalIndex, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover) {
        // The results map can be transiently empty during a refresh while the list
        // still repaints; render a pending placeholder rather than crashing inside
        // the cell renderer.
        final @NotNull TestRunItems runItem = editor.runItem(tc.getId())
                .orElseGet(() -> TestRunItems.builder().id(tc.getId()).tc(tc).build());

        card.updateData(globalIndex, editor.getSelectedDetails(), runItem, editor.cardTitle(globalIndex, tc));
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}