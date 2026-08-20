package org.testin.editor.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.editor.test.TestCard;
import org.testin.editor.test.TestEditor;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;

public class TestListRenderer extends AbstractListRenderer<TestinEditor> {
    private final @NotNull TestCard card;

    public TestListRenderer(final @NotNull Project p, final @NotNull TestEditor editor) {
        super(editor);
        this.card = new TestCard(p);
    }

    @Override
    protected @NotNull TestCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int globalIndex, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover) {

        card.updateData(globalIndex, tc, editor.getSelectedDetails(), editor.cardTitle(globalIndex, tc));
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}