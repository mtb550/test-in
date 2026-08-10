package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Keystroke-to-runnable binding used by the bulk editors' keymaps.
 */
final class KeyAction extends DumbAwareAction {

    private final @NotNull Runnable body;

    private KeyAction(final @NotNull Runnable body) {
        this.body = body;
    }

    static void register(final @NotNull Runnable body, final @NotNull KeyStroke keyStroke, final @NotNull JComponent component) {
        new KeyAction(body).registerCustomShortcutSet(new CustomShortcutSet(keyStroke), component);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        body.run();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
