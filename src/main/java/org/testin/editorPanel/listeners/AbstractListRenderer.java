package org.testin.editorPanel.listeners;

import com.intellij.util.ui.JBUI;
import org.testin.editorPanel.EditorColors;
import org.testin.editorPanel.IEditor;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public abstract class AbstractListRenderer<U extends IEditor> implements ListCellRenderer<TestCaseDto> {

    private static final Border SELECTED_BORDER = JBUI.Borders.customLine(EditorColors.SELECTION_BORDER, 1);
    private static final Border UNSELECTED_BORDER = JBUI.Borders.empty(1);
    protected final U editor;

    public AbstractListRenderer(final U editor) {
        this.editor = editor;
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends TestCaseDto> list, final TestCaseDto tc, final int index, final boolean isSelected, final boolean cellHasFocus) {
        final int globalIndex = ((editor.getCurrentPage() - 1) * editor.getPageSize()) + index;

        final boolean isRowHovered = (index == editor.getHoveredIndex());
        final String hover = isRowHovered ? editor.getHoveredIconAction() : null;

        final JComponent card = bindDataAndGetCard(list, tc, globalIndex, isSelected, isRowHovered, hover);

        card.setBorder(isSelected ? SELECTED_BORDER : UNSELECTED_BORDER);

        return card;
    }

    protected abstract JComponent bindDataAndGetCard(JList<? extends TestCaseDto> list, TestCaseDto tc, int globalIndex, boolean isSelected, boolean isRowHovered, String hover);
}
