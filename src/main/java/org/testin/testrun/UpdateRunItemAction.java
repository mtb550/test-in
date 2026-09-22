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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinData;
import org.testin.editor.run.RunEditor;
import org.testin.logger.Logger;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testrun.failure.FailedResultDialog;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.Optional;

// UC-EDITOR-PANEL-040
public class UpdateRunItemAction extends DumbAwareAction {
    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        final @NotNull Optional<TestCaseDto> selected = TestinData.singleSelectedTestCase(e);
        final @NotNull Optional<RunEditor> runEditor = TestinData.runEditor(e);
        if (selected.isEmpty() || runEditor.isEmpty()) return;

        edit(p, runEditor.orElseThrow(), selected.orElseThrow());
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167
    private void edit(final @NotNull Project p, final @NotNull RunEditor runEditor, final @NotNull TestCaseDto testCase) {
        final @NotNull Optional<TestRunItems> found = runEditor.runItem(testCase.getId());
        if (found.isEmpty()) return;

        final @NotNull TestRunItems runItem = found.orElseThrow();

        if (runItem.isRemoved()) {
            Services.getInstance(p, RunStatusService.class).refuseRemoved(p);
            return;
        }

        Logger.trace("update test run item for: " + testCase.getDescription());

        new FailedResultDialog(p, runEditor.getParent().getPath(), runItem, fields -> {
            if (!Services.getInstance(p, RunStatusService.class).recordFailureDetails(p, runEditor.getParent().getPath(), testCase.getId(), fields))
                return;

            ApplicationManager.getApplication().invokeLater(() -> {
                runEditor.refreshView();

                ViewToolWindowFactory.refreshIfShowing(p, List.of(testCase));
            });

            Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("run.item.updated"));
        }).show();
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-168
    @Override
    public void update(final @NotNull AnActionEvent e) {
        GrayWithReason.unless(this, e, TestinData.runEditor(e)
                        .flatMap(runEditor -> TestinData.singleSelectedTestCase(e).flatMap(tc -> runEditor.runItem(tc.getId())))
                        .filter(item -> item.shownStatus() == TestStatus.FAILED)
                        .isPresent(),
                Bundle.message("run.item.details.disabled.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
