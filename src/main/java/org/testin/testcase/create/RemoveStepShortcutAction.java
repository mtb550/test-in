package org.testin.testcase.create;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Removes the step row whose field currently owns the focus
 * (see Shortcuts.CreateTestCaseRemoveStep).
 */
@AllArgsConstructor
final class RemoveStepShortcutAction extends DumbAwareAction {

    private final @NotNull JComponent stepField;
    private final @NotNull Runnable removeStep;

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (UIUtil.isDescendingFrom(focusOwner, stepField)) {
            removeStep.run();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
