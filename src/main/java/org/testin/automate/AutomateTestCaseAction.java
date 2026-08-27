package org.testin.automate;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;

import java.util.Optional;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class AutomateTestCaseAction extends AbstractProjectAction {

    private static final @NotNull KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK);
    private final @NotNull JBList<TestCaseDto> list;

    public AutomateTestCaseAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Automate Test Case", "Generate automation code for the selected test case", AllIcons.Actions.IntentionBulb);
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        // update() disables the action on an empty selection, but the shortcut
        // and the selection can race, so Swing can still answer with nothing.
        /// TODO: to be implemented by integrating to pi agent automatic, next release
        Optional.ofNullable(list.getSelectedValue()).ifPresent(tc -> Logger.info(tc.getDescription()));

        // Says so until then. This is the one action in the menu that changes
        // nothing, and silence here reads as a bug rather than as unbuilt: after
        // #62 every other action confirms itself, so the odd one out is the one
        // that answers with nothing at all (#66, F4).
        Services.getInstance(p, Notifier.class).softRefuse(p, "Not built yet",
                "Generating automation code for a test case is coming in a later release.");
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
