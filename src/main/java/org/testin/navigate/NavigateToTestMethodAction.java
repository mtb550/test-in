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

package org.testin.navigate;

import org.testin.codegen.CodeOn;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.CardHoverAction;
import org.testin.model.dto.TestCaseDto;

// UC-CODEGEN-006
public class NavigateToTestMethodAction extends DumbAwareAction {
    public static void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        if (!CodeOn.isOnOrWarn(p)) return;

        CodeNavigation.available().toCode(p, tc);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.selectedCases(e).stream().findFirst().ifPresent(tc -> execute(p, tc));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (!CardHoverAction.NAVIGATE_TO_TEST_METHOD.enableOrExplain(e.getPresentation())) return;

        e.getPresentation().setEnabled(!TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
