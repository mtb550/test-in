package org.testin.navigate;

import org.testin.editor.CardHoverAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.OptionalPlugin;
import org.testin.util.Shortcuts;


public class NavigateToCodeAction extends AbstractProjectAction {
    private final @NotNull JBList<TestCaseDto> list;

    public NavigateToCodeAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, CardHoverAction.NAVIGATE_TO_TEST_METHOD.getTooltip(), "Jump to the automated test case", AllIcons.General.ArrowRight);
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.NavigateToCode.getCustomShortcut(), list);
    }

    /**
     * Static because it reads nothing of the action it sits on. The two hover
     * icons used to build one of these just to reach it, which registered this
     * action's shortcut set on the list again on every single click.
     */
    public static void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        if (!OptionalPlugin.JAVA.isAvailableOrWarn(p)) return;

        CodeNavigation.available().toCode(p, tc);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute(p, list.getSelectedValue());
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
