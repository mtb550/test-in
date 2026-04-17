package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.CodeNavigator;
import testGit.util.KeyboardSet;

public class NavigateToCode extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public NavigateToCode(final JBList<TestCaseDto> list) {
        super("Navigate to Code", "Jump to the automated test case", AllIcons.General.ArrowRight);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.NavigateToCode.getShortcut(), list);
    }

    public static void execute(final TestCaseDto tc) {
        if (tc == null) return;
        System.out.println("[TRACE] Navigating to: " + tc.getDescription());
        System.out.println("[TRACE] AutoRef: " + tc.getFqcn());
        CodeNavigator.toCode(tc.getFqcn(), tc.getDescription());
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        execute(list.getSelectedValue());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
