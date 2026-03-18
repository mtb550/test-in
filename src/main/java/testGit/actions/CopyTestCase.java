package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class CopyTestCase extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public CopyTestCase(final JBList<TestCaseDto> list) {
        super("Copy", "Copy test case", AllIcons.Actions.Copy);
        this.list = list;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCaseDto tc = list.getSelectedValue();
        String text = "Title: " + tc.getTitle() + "\nSteps: " + tc.getSteps() + "\nExpected: " + tc.getExpected();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
