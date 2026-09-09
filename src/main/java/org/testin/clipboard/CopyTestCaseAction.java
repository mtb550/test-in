package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CopyTestCaseAction extends AbstractProjectAction {
    private final @NotNull JBList<TestCaseDto> list;

    public CopyTestCaseAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Copy", "Copy test case", AllIcons.Actions.Copy);
        this.list = list;
        registerCustomShortcutSet(Shortcuts.CopyItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selected = list.getSelectedValuesList();
        if (selected.isEmpty()) return;

        // One case per block, blank line between them, so a multi-case copy reads
        // as separate details and not one run-on.
        final @NotNull String text = selected.stream()
                .map(this::detailsOf)
                .collect(Collectors.joining("\n\n"));

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);

        // Not just "Copied": the same list also offers Copy Node, which puts the
        // case itself on the clipboard rather than its readable details (#62).
        Services.getInstance(p, Notifier.class).softShow(p, selected.size() == 1 ? "Details copied" : "Details copied " + selected.size());
    }

    /**
     * UC-EDITOR-PANEL-026, Rule-EDITOR-PANEL-008.
     * <p>
     * The test case as text: every field the tester wrote on it, one to a line,
     * captioned.
     * <p>
     * Can.COPY was declared on the description and on nothing else, so this put
     * one line on the clipboard while the message said "Details copied" (#197).
     * <p>
     * A field the tester left empty is not a line. A block of "Module:" and
     * "Reference:" with nothing after them is the paste saying what the test
     * case does not have, which is the details panel's own rule for a blank row.
     */
    private @NotNull String detailsOf(final @NotNull TestCaseDto tc) {
        return Arrays.stream(TestEditorAttributes.values())
                .filter(a -> a.can(Can.COPY))
                .filter(attr -> !attr.gridValue(tc).isBlank())
                // The colon belongs to this line, not to the caption. It used to be
                // part of the name, so the view panel drew test case rows with
                // one and run rows without in the same column (#232).
                .map(attr -> attr.getName() + ": " + attr.gridValue(tc))
                .collect(Collectors.joining("\n"));
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
