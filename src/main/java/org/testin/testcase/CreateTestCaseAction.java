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

package org.testin.testcase;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.codegen.GenType;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.create.CreateTestCaseDialog;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// UC-EDITOR-PANEL-005
public class CreateTestCaseAction extends DumbAwareAction {
    // UC-EDITOR-PANEL-005
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.editor(e)
                .filter(editor -> editor.getParent().isTestCaseContainer())
                .ifPresent(editor -> openCreateDialog(p, editor, (TestSetDirectoryDto) editor.getParent()));
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-008, Rule-EDITOR-PANEL-030
    public static void openCreateDialog(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull TestSetDirectoryDto dir) {
        new CreateTestCaseDialog(p, dir, tc -> {
            tc.setParent(dir);

            final @NotNull List<TestCaseDto> affectedNodes = List.of(tc);

            final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(affectedNodes);
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, dir.getPath(), ids);

            editor.appendNewTestCase(tc, () -> {
                TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.create"), affectedNodes), before, TestCaseSnapshot.of(p, dir.getPath(), ids));

                ApplicationManager.getApplication().invokeLater(() -> {
                    Services.getInstance(p, Notifier.class).softShow(p, Done.CREATED);
                    GenType.CREATE_TEST_CASE.getAction().execute(p, tc);
                });
            });
            Services.getInstance(p, TestCaseValues.class).addNewItems(affectedNodes);
        }).show();
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-214
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<TestinEditor> editor = TestinData.editor(e);
        final boolean holdsCases = editor.filter(open -> open.getParent().isTestCaseContainer()).isPresent();

        final boolean loading = editor.filter(TestinEditor::isLoading).isPresent();
        final boolean enabled = holdsCases && !loading;

        e.getPresentation().setEnabled(enabled);

        if (!enabled && editor.isPresent()) {
            e.getPresentation().setDescription(loading
                    ? Bundle.message("create.case.still.loading")
                    : Bundle.message("create.case.disabled.description"));
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
