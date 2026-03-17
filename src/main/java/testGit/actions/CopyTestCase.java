package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCaseJsonMapper;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class CopyTestCase extends DumbAwareAction {
    TestCaseJsonMapper tc;

    public CopyTestCase(TestCaseJsonMapper tc) {
        super("Copy", "Copy test case", AllIcons.Actions.Copy);
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String text = "Title: " + tc.getTitle() + "\nSteps: " + tc.getSteps() + "\nExpected: " + tc.getExpected();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
