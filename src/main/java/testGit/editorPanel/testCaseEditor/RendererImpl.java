package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import testGit.pojo.mappers.TestCaseJsonMapper;

import javax.swing.*;
import java.awt.*;

public class RendererImpl implements ListCellRenderer<TestCaseJsonMapper> {
    private final TestCaseCard rendererCard = new TestCaseCard();
    private final FileEditorImpl editor;

    public RendererImpl(FileEditorImpl editor) {
        this.editor = editor;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TestCaseJsonMapper> list, TestCaseJsonMapper tc, int index, boolean isSelected, boolean cellHasFocus) {
        int globalIndex = ((editor.getCurrentPage() - 1) * editor.getPageSize()) + index;

        rendererCard.updateData(globalIndex, tc, editor.isShowGroups(), editor.isShowPriority(), editor.getSelectedDetails());

        rendererCard.setBorder(isSelected ?
                JBUI.Borders.customLine(JBColor.blue, 1) :
                JBUI.Borders.empty(1));

        return rendererCard;
    }
}