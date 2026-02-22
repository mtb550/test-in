package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class CopyTestCase extends DumbAwareAction {
    TestCase tc;

    public CopyTestCase(TestCase tc) {
        super("Copy", "Copy test case", AllIcons.Actions.Copy);
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String text = "Title: " + tc.getTitle() + "\nSteps: " + tc.getSteps() + "\nExpected: " + tc.getExpectedResult();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
