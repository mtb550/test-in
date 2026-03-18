package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import testGit.pojo.mappers.TestCase;

import javax.swing.*;
import java.awt.*;

public class TestListRenderer implements ListCellRenderer<TestCase> {
    private final TestCard rendererCard = new TestCard();

    private final TestEditorUI ui;

    public TestListRenderer(TestEditorUI ui) {
        this.ui = ui;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TestCase> list, TestCase tc, int index, boolean isSelected, boolean cellHasFocus) {

        int globalIndex = ((ui.getCurrentPage() - 1) * ui.getPageSize()) + index;

        rendererCard.updateData(globalIndex, tc, ui.isShowGroups(), ui.isShowPriority(), ui.getSelectedDetails());

        rendererCard.setBorder(isSelected ?
                JBUI.Borders.customLine(JBColor.blue, 1) :
                JBUI.Borders.empty(1));

        return rendererCard;
    }
}