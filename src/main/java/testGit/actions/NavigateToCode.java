package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.util.CodeNavigator;
import testGit.util.KeyboardSet;

public class NavigateToCode extends DumbAwareAction {
    private final TestCaseJsonMapper tc;

    public NavigateToCode(TestCaseJsonMapper tc, JBList<TestCaseJsonMapper> list) {
        super("Navigate to Code", "Jump to the automated test case", AllIcons.General.ArrowRight);
        this.tc = tc;
        this.registerCustomShortcutSet(KeyboardSet.NavigateToCode.getShortcut(), list);
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        System.out.println("[TRACE] Navigating to: " + tc.getTitle());
        System.out.println("[TRACE] AutoRef: " + tc.getAutoRef());

        CodeNavigator.toCode(tc.getAutoRef(), tc.getTitle());
    }
}
