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

package org.testin.clipboard;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.TestinEditor;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;

public class CutTestCaseNodeAction extends DumbAwareAction {
    // UC-EDITOR-PANEL-016
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        final @NotNull Optional<TestinEditor> found = TestinData.editor(e);
        if (p == null || found.isEmpty()) return;

        final @NotNull TestinEditor editor = found.orElseThrow();
        final @NotNull List<TestCaseDto> selectedTestCases = TestinData.selectedCases(e);

        if (!selectedTestCases.isEmpty()) {
            try {
                final @NotNull String json = Services.getInstance(p, Mapper.class).writeValueAsString(selectedTestCases);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

                Services.getInstance(p, CutState.class).cut(editor, selectedTestCases);

                editor.refreshView();

                Services.getInstance(p, Notifier.class).softShowCounted(p, Done.CUT, selectedTestCases.size());

            } catch (final Exception ex) {
                Logger.error("Cut Node failed: " + FailureText.of(ex));
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("clipboard.cut.failed.title"), FailureText.of(ex));
            }
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-EDITOR-PANEL-214
        if (TestinData.editor(e).filter(editor -> !editor.getParent().isTestCaseContainer()).isPresent()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("cut.case.disabled.description"));
            return;
        }

        e.getPresentation().setEnabled(!TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
