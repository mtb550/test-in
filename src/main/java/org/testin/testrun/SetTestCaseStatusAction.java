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
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testrun.failure.FailedResultDialog;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public class SetTestCaseStatusAction extends DumbAwareAction {
    @Getter
    private final @NotNull TestStatus status;

    public SetTestCaseStatusAction(final @NotNull TestStatus status) {
        super(status.getLabel());
        this.status = status;

        getTemplatePresentation().setDescription(Bundle.message("run.case.status.description", status.getLabel()));
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033, UC-EDITOR-PANEL-034
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        final @NotNull List<TestCaseDto> selectedItems = TestinData.selectedTestCases(e);
        if (p == null || selectedItems.isEmpty()) return;

        TestinData.runEditor(e).ifPresent(editor -> record(p, editor, selectedItems));
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033
    private void record(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        if (status.isCollectsFailureDetails() && selectedItems.size() == 1) {
            final @NotNull Optional<TestRunItems> runItem = editor.runItem(selectedItems.getFirst().getId())
                    .filter(item -> !item.isRemoved());

            if (runItem.isPresent()) {
                new FailedResultDialog(p, editor.getParent().getPath(), runItem.orElseThrow(), fields -> {
                    if (Services.getInstance(p, RunStatusService.class).recordFailureDetails(p, editor.getParent().getPath(), selectedItems.getFirst().getId(), fields)) {
                        applyStatus(p, editor, selectedItems);
                    }
                }).show();
                return;
            }
        }

        applyStatus(p, editor, selectedItems);
    }

    private void applyStatus(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, selectedItems, status);
    }

    // UC-EDITOR-PANEL-032
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.runEditor(e).isPresent()
                && !TestinData.selectedTestCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
