package org.testin.testrun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.testrun.create.FailedResultDialog;
import org.testin.util.Shortcuts;

import java.util.Optional;
import java.util.List;

/**
 * Sets the selected test cases to any user-settable {@link TestStatus}. The
 * constant carries the label, icon, shortcut and whether details are collected
 * first — one action for all statuses instead of one class per status.
 */
public class SetTestCaseStatusAction extends AbstractProjectAction {
    private final @NotNull TestinEditor editor;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull TestStatus status;

    public SetTestCaseStatusAction(final @NotNull Project p, final @NotNull TestinEditor editor,
                                   final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status,
                                   final @NotNull TestStatus.MenuEntry entry) {
        super(p, status.getLabel(), "Set test case status to " + status.getLabel(), entry.icon());
        this.editor = editor;
        this.list = list;
        this.status = status;

        this.registerCustomShortcutSet(Shortcuts.customShortcut(entry.shortcut()), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        // Single selection of a run item: collect failure details first, apply after the dialog closes.
        if (status.isCollectsFailureDetails() && editor instanceof RunEditor runEditor && selectedItems.size() == 1) {
            final @NotNull Optional<TestRunItems> runItem = runEditor.runItem(selectedItems.getFirst().getId())
                    .filter(item -> !item.isRemoved());

            if (runItem.isPresent()) {
                new FailedResultDialog(p, runItem.get(), this::applyStatus).show();
                return;
            }
        }

        applyStatus();
    }

    private void applyStatus() {
        Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, list, status);
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
