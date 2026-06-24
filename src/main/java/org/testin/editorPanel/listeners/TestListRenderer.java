package org.testin.editorPanel.listeners;

import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.testEditor.TestCard;
import org.testin.editorPanel.testEditor.TestEditorUI;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;

public class TestListRenderer extends AbstractListRenderer<IEditorUI> {
    private final TestCard card;

    public TestListRenderer(final TestEditorUI ui) {
        super(ui);
        this.card = new TestCard(ui.getProject());
    }

    @Override
    protected JComponent bindDataAndGetCard(JList<? extends TestCaseDto> list, TestCaseDto tc, int globalIndex, boolean isSelected, boolean isRowHovered, String hover) {
        final boolean isUnsorted = ui.getUnsortedIds().contains(tc.getId());

        card.updateData(globalIndex, tc, ui.getSelectedDetails(), isUnsorted);
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListFont(list.getFont());

        return card;
    }
}