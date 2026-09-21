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

package org.testin.runner;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.CardHoverAction;
import org.testin.model.dto.TestCaseDto;

import java.util.List;

public class RunTestMethodAction extends DumbAwareAction {
    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        final @NotNull List<TestCaseDto> selected = TestinData.selectedCases(e);
        final @NotNull CardHoverAction gesture = CardHoverAction.runSlot(p, selected);

        if (gesture == CardHoverAction.RUN_TEST_METHOD) {
            TestinData.editor(e).ifPresent(ui -> selected.forEach(tc -> ui.launching(tc.getId())));
        }

        gesture.execute(p, selected);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-036
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final @NotNull List<TestCaseDto> selected = TestinData.selectedCases(e);
        final @NotNull CardHoverAction offered = CardHoverAction.runSlot(p, selected);

        e.getPresentation().setText(offered.getTooltip());
        e.getPresentation().setIcon(offered.getIcon());

        if (!offered.enableOrExplain(e.getPresentation())) return;

        e.getPresentation().setEnabled(!selected.isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
