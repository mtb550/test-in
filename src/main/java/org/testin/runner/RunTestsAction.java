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

import org.testin.codegen.CodeOn;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.TestinEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.editor.TestinEditors;

import java.util.List;
import java.util.Optional;

public class RunTestsAction extends DumbAwareAction {
    // Rule-TREE-PANEL-079
    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (!CodeOn.enableOrExplain(this, e)) return;
        if (!OptionalPlugin.TESTNG.enableOrExplain(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(runnable(e).isPresent() || selectedRun(e).isPresent());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        selectedRun(e).ifPresentOrElse(run -> openAndRun(p, run), () -> runnable(e).ifPresent(dir -> run(p, dir)));
    }

    private @NotNull Optional<DirectoryDto> runnable(final @NotNull AnActionEvent e) {
        return TestinData.firstSelected(e, DirectoryDto.class).filter(DirectoryDto::isTestCaseContainer);
    }

    private @NotNull Optional<TestRunDirectoryDto> selectedRun(final @NotNull AnActionEvent e) {
        return TestinData.firstSelected(e, TestRunDirectoryDto.class)
                .filter(TestRunDirectoryDto::isStillOpen);
    }

    private void openAndRun(final @NotNull Project p, final @NotNull TestRunDirectoryDto run) {
        Services.getInstance(p, TestinEditors.class).openThen(p, run, TestinEditor::runWhenLoaded);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031
    private void run(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull List<TestCaseDto> cases = Services.getInstance(p, ProjectIndexer.class).getTestCasesUnder(dir);

        if (cases.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NOTHING_TO_RUN, dir.getName());
            return;
        }

        Logger.info("Running " + dir.getName() + " with " + cases.size() + " test case(s)");

        RunTestCases.run(p, cases);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
