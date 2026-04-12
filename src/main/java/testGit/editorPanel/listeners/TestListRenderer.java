package testGit.editorPanel.listeners;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import testGit.editorPanel.BaseEditorUI;
import testGit.editorPanel.testCaseEditor.TestCard;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public class TestListRenderer implements ListCellRenderer<TestCaseDto> {
    private final TestCard rendererCard = new TestCard();
    private final BaseEditorUI ui;

    public TestListRenderer(final BaseEditorUI ui) {
        this.ui = ui;
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends TestCaseDto> list, final TestCaseDto tc, final int index, final boolean isSelected, final boolean cellHasFocus) {

        final int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;
        final boolean isUnsorted = ui.getUnsortedIds().contains(tc.getId());

        rendererCard.updateData(globalIndex, tc, ui.getSelectedDetails(), isUnsorted);

        final boolean isRowHovered = (index == ui.getHoveredIndex());
        final String hover = isRowHovered ? ui.getHoveredIconAction() : null;

        rendererCard.setActionsState(isSelected, isRowHovered, hover);

        rendererCard.setBorder(isSelected ?
                JBUI.Borders.customLine(JBColor.blue, 1) :
                JBUI.Borders.empty(1));

        return rendererCard;
    }
}