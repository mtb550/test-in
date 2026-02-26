package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.*;

public class RendererImpl implements ListCellRenderer<TestCase> {
    private final TestCaseCard rendererCard = new TestCaseCard();
    private final FileEditorImpl editor;

    public RendererImpl(FileEditorImpl editor) {
        this.editor = editor;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TestCase> list, TestCase tc, int index, boolean isSelected, boolean cellHasFocus) {
        rendererCard.updateData(index, tc, editor.isShowGroups(), editor.isShowPriority(), editor.getSelectedDetails());

        rendererCard.setBorder(isSelected ?
                JBUI.Borders.customLine(JBColor.blue, 1) :
                JBUI.Borders.empty(1));
        return rendererCard;
    }
}