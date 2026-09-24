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

package org.testin.testrun.failure;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogComponent;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public final class FailureFields {
    private final @NotNull ActualResultSection actualResult;
    private final @NotNull ScreenshotsSection screenshots;

    private final @NotNull List<FailureSection> sections;

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-147
    public FailureFields(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunItems runItem) {
        actualResult = new ActualResultSection(p, runItem);
        screenshots = new ScreenshotsSection(p, runPath, runItem);

        sections = List.of(actualResult, new BugSeveritySection(runItem), new BugPrioritySection(runItem), new StacktraceSection(runItem), screenshots);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148
    public @NotNull List<? extends ComponentDialogBase<?>> components() {
        return sections.stream().map(FailureSection::getComponent).toList();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145
    public void applyTo(final @NotNull TestRunItems runItem) {
        sections.forEach(section -> section.applyTo(runItem));
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public void storePasted(final @NotNull Function<List<byte[]>, List<String>> store) {
        screenshots.storePasted(store);
    }

    // Rule-EDITOR-PANEL-202, Rule-EDITOR-PANEL-219, Rule-INTERNAL-097
    public void onResized(final @NotNull Runnable resized) {
        screenshots.onChange(resized);
        actualResult.growsWith(resized);
    }

    public @NotNull DialogComponent actualResult() {
        return actualResult.getComponent().getComponent();
    }
}
