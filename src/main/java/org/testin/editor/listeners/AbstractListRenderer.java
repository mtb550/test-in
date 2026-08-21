package org.testin.editor.listeners;

import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.BaseCard;
import org.testin.editor.EditorColors;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.border.Border;

@AllArgsConstructor
public abstract class AbstractListRenderer<U extends TestinEditor> implements ListCellRenderer<TestCaseDto> {

    private static final @NotNull Border SELECTED_BORDER = JBUI.Borders.customLine(EditorColors.SELECTION_BORDER, 1);
    private static final @NotNull Border UNSELECTED_BORDER = JBUI.Borders.empty(1);
    protected final @NotNull U editor;

    @Override
    public @NotNull BaseCard getListCellRendererComponent(final JList<? extends TestCaseDto> list, final TestCaseDto tc, final int index, final boolean isSelected, final boolean cellHasFocus) {
        final int globalIndex = editor.globalIndex(index);

        final boolean isRowHovered = (index == editor.getHoveredIndex());
        final String hover = isRowHovered ? editor.getHoveredIconAction() : "";

        final BaseCard card = bindDataAndGetCard(list, tc, globalIndex, isSelected, isRowHovered, hover);

        card.setBorder(isSelected ? SELECTED_BORDER : UNSELECTED_BORDER);

        return card;
    }

    protected abstract @NotNull BaseCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int globalIndex, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover);
}

