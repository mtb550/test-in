package org.testin.navigate;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.CardHoverAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.OptionalPlugin;

/**
 * UC-CODEGEN-005.
 * <p>
 * Declared in {@code plugin.xml} (#119) with Shift+F5 as its default, which is
 * free in IntelliJ's keymap. A tester would look for this in Find Action, and it
 * is the one gesture that crosses from a test case to the code behind it.
 */
public class NavigateToCodeAction extends DumbAwareAction {

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
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.selectedCases(e).stream().findFirst().ifPresent(tc -> execute(p, tc));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Grayed with the reason without the Java plugin, rather than left out of
        // the menu (#248).
        if (!CardHoverAction.NAVIGATE_TO_TEST_METHOD.enableOrExplain(e.getPresentation())) return;

        e.getPresentation().setEnabled(!TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
