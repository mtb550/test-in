package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;
import testGit.util.KeyboardSet;

public class GenerateTestCase extends DumbAwareAction {
    private final TestCase tc;

    public GenerateTestCase(TestCase tc, final JBList<TestCase> list) {
        super("Generate Test", "", AllIcons.Actions.IntentionBulb);
        this.tc = tc;
        this.registerCustomShortcutSet(KeyboardSet.GenerateTestCase.get(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /// to be implemented
        System.out.println(tc.getTitle());
    }
}
