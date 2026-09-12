/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.testrun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.TestinEditor;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testrun.create.FailedResultDialog;
import org.testin.util.Bundle;

import java.util.Optional;
import java.util.List;

/**
 * Sets the selected test cases to any user-settable {@link TestStatus}. The
 * constant carries the label, icon, shortcut and whether details are collected
 * first - one action for all statuses instead of one class per status.
 * <p>
 * Built by {@link SetTestCaseStatusGroup}, which is what {@code plugin.xml}
 * declares (#119). Which status it records is the only thing it carries: the
 * editor and the selection come from the keystroke.
 * <p>
 * Its key does not: P, F and B are bare letters, and a bare letter in the keymap
 * would answer everywhere in the IDE, including while somebody is typing. The
 * group puts them on the run editor's list, where a verdict is a gesture of that
 * list rather than a command.
 */
public class SetTestCaseStatusAction extends DumbAwareAction {
    @Getter
    private final @NotNull TestStatus status;

    public SetTestCaseStatusAction(final @NotNull TestStatus status) {
        super(status.getLabel(), Bundle.message("run.case.status.description", status.getLabel()), status.getMenuEntry().icon());
        this.status = status;
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033, UC-EDITOR-PANEL-034
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        final @NotNull List<TestCaseDto> selectedItems = TestinData.selectedCases(e);
        if (p == null || selectedItems.isEmpty()) return;

        final @NotNull Optional<TestinEditor> editor = TestinData.editor(e);
        if (editor.isEmpty()) return;

        record(p, editor.orElseThrow(), selectedItems);
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033
    private void record(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        // Single selection of a run item: collect failure details first, apply after the dialog closes.
        if (status.isCollectsFailureDetails() && editor instanceof RunEditor runEditor && selectedItems.size() == 1) {
            final @NotNull Optional<TestRunItems> runItem = runEditor.runItem(selectedItems.getFirst().getId())
                    .filter(item -> !item.isRemoved());

            if (runItem.isPresent()) {
                new FailedResultDialog(p, runItem.orElseThrow(), () -> applyStatus(p, editor, selectedItems)).show();
                return;
            }
        }

        applyStatus(p, editor, selectedItems);
    }

    private void applyStatus(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, selectedItems, status);
    }

    /**
     * UC-EDITOR-PANEL-032.
     * <p>
     * On a selected case in a run editor, and gray everywhere else. A verdict
     * belongs to a run, and the test set editor answers the same data key with
     * no run behind it (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.editor(e).filter(RunEditor.class::isInstance).isPresent()
                && !TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
