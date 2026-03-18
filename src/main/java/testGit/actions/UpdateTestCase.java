package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

public class UpdateTestCase extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public UpdateTestCase(final JBList<TestCaseDto> list) {
        super("Update", "Update test case", AllIcons.Actions.Edit);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TestCaseDto tc = list.getSelectedValue();
        ///  to be implemented
        /// show view details in view panel
    }
}

