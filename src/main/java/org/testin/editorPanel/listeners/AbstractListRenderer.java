package org.testin.editorPanel.listeners;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.testin.editorPanel.IEditorUI;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public abstract class AbstractListRenderer<U extends IEditorUI> implements ListCellRenderer<TestCaseDto> {

    private static final Border SELECTED_BORDER = JBUI.Borders.customLine(JBColor.blue, 1);
    private static final Border UNSELECTED_BORDER = JBUI.Borders.empty(1);
    protected final U ui;

    public AbstractListRenderer(final U ui) {
        this.ui = ui;
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends TestCaseDto> list, final TestCaseDto tc, final int index, final boolean isSelected, final boolean cellHasFocus) {
        final int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;

        final boolean isRowHovered = (index == ui.getHoveredIndex());
        final String hover = isRowHovered ? ui.getHoveredIconAction() : null;

        final JComponent card = bindDataAndGetCard(list, tc, globalIndex, isSelected, isRowHovered, hover);

        card.setBorder(isSelected ? SELECTED_BORDER : UNSELECTED_BORDER);

        return card;
    }

    protected abstract JComponent bindDataAndGetCard(JList<? extends TestCaseDto> list, TestCaseDto tc, int globalIndex, boolean isSelected, boolean isRowHovered, String hover);
}