package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCase;
import testGit.util.KeyboardSet;

public class GenerateTestCase extends DumbAwareAction {
    private final JBList<TestCase> list;

    public GenerateTestCase(final JBList<TestCase> list) {
        super("Generate Test", "", AllIcons.Actions.IntentionBulb);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.GenerateTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCase tc = list.getSelectedValue();

        /// to be implemented
        System.out.println(tc.getTitle());
    }
}
