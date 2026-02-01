package testGit.actions.editorPanel;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;

import javax.swing.*;

public class DeleteTestCaseAction extends AnAction {
    TestCase tc;
    DefaultListModel<TestCase> model;

    public DeleteTestCaseAction(TestCase tc, DefaultListModel<TestCase> model) {
        super("🗑 Delete");
        this.tc = tc;
        this.model = model;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        int idx = model.indexOf(tc);
        if (idx >= 0) {
            model.remove(idx);
        }
    }
}
