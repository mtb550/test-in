package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

public class GenerateTestCase extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public GenerateTestCase(final JBList<TestCaseDto> list) {
        super("Generate Test", "", AllIcons.Actions.IntentionBulb);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.GenerateTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCaseDto tc = list.getSelectedValue();

        /// to be implemented
        System.out.println(tc.getTitle());
    }
}
