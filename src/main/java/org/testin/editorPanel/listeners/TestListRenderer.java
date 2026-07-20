package org.testin.editorPanel.listeners;

import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.testEditor.TestCard;
import org.testin.editorPanel.testEditor.TestEditor;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;

public class TestListRenderer extends AbstractListRenderer<IEditor> {
    private final TestCard card;

    public TestListRenderer(final TestEditor editor) {
        super(editor);
        this.card = new TestCard(editor.getProject());
    }

    @Override
    protected JComponent bindDataAndGetCard(JList<? extends TestCaseDto> list, TestCaseDto tc, int globalIndex, boolean isSelected, boolean isRowHovered, String hover) {
        final boolean isUnsorted = editor.getUnsortedIds().contains(tc.getId());

        card.updateData(globalIndex, tc, editor.getSelectedDetails(), isUnsorted);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}