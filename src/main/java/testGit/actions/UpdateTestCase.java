package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCase;
import testGit.util.KeyboardSet;

public class UpdateTestCase extends DumbAwareAction {
    private final JBList<TestCase> list;

    public UpdateTestCase(final JBList<TestCase> list) {
        super("Update", "Update test case", AllIcons.Actions.Edit);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TestCase tc = list.getSelectedValue();
        ///  to be implemented
        /// show view details in view panel
    }
}

