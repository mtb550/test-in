package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.dialogs.ShortcutMenuPopup;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CopyTestCaseAction extends AbstractProjectAction {
    private final @NotNull JBList<TestCaseDto> list;

    public CopyTestCaseAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Copy", "Copy test case", AllIcons.Actions.Copy);
        this.list = list;
        registerCustomShortcutSet(Shortcuts.CopyItem.getCustomShortcut(), list);
    }

    /**
     * UC-EDITOR-PANEL-014, Rule-EDITOR-PANEL-207.
     * <p>
     * Asks which value, rather than assuming.
     * <p>
     * CTRL+C copied every field the tester wrote, which is one answer to a
     * question with fourteen. A tester who wants the class name for a stack
     * trace, or the identity to search the code with, had to copy the lot and
     * cut it down by hand.
     * <p>
     * A menu rather than a two-stroke key. The popup binds each row's letter, so
     * CTRL+C then D is as quick as a chord for somebody who knows it - and shows
     * the letters to somebody who does not, instead of doing nothing. It also
     * keeps CTRL+C a plain shortcut rather than the first half of one, which
     * would make the platform wait to see whether a second key follows.
     * <p>
     * All Details is the first row and starts selected, so CTRL+C then ENTER is
     * the gesture this key always was.
     */
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selected = list.getSelectedValuesList();
        if (selected.isEmpty()) return;

        new ShortcutMenuPopup<>(p, "Copy", CopyChoice.values(), choice -> copy(choice, selected)).show();
    }

    private void copy(final @NotNull CopyChoice choice, final @NotNull List<TestCaseDto> selected) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(choice.from(selected)), null);

        // The value's own name, not just "Copied": the same list also offers Copy
        // Node, which puts the case itself on the clipboard rather than anything
        // readable, and now thirteen other things besides (#62).
        Services.getInstance(p, Notifier.class).softShow(p, choice.copiedMessage(selected.size()));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
