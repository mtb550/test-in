package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.util.KeyboardSet;

public class UpdateTestCase extends DumbAwareAction {
    TestCaseJsonMapper tc;

    public UpdateTestCase(final TestCaseJsonMapper tc, final JBList<TestCaseJsonMapper> list) {
        super("Update", "Update test case", AllIcons.Actions.Edit);
        this.tc = tc;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ///  to be implemented
        /// show view details in view panel
    }
}

