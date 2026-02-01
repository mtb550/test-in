package testGit.actions.editorPanel;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class CopyTestCaseAction extends AnAction {
    TestCase tc;

    public CopyTestCaseAction(TestCase tc) {
        super("📋 Copy");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String text = "Title: " + tc.getTitle() + "\nSteps: " + tc.getSteps() + "\nExpected: " + tc.getExpectedResult();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
