package org.testin.editorPanel.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.testEditor.TestCard;
import org.testin.editorPanel.testEditor.TestEditor;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;

public class TestListRenderer extends AbstractListRenderer<IEditor> {
    private final @NotNull TestCard card;

    public TestListRenderer(final @NotNull Project p, final @NotNull TestEditor editor) {
        super(editor);
        this.card = new TestCard(p);
    }

    @Override
    protected @NotNull JComponent bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int globalIndex, final boolean isSelected, final boolean isRowHovered, final @Nullable String hover) {
        final boolean isUnsorted = editor.getUnsortedIds().contains(tc.getId());

        card.updateData(globalIndex, tc, editor.getSelectedDetails(), isUnsorted);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}