package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;
import testGit.util.ShortcutSet;

public class UpdateTestCase extends DumbAwareAction {
    TestCase tc;

    public UpdateTestCase(final TestCase tc, final JBList<TestCase> list) {
        super("Update", "Update test case", AllIcons.Actions.Edit);
        this.tc = tc;
        this.registerCustomShortcutSet(ShortcutSet.UpdateTestCase.get(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ///  to be implemented
        /// show view details in view panel
    }
}

