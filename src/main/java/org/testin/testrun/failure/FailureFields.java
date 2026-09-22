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
import org.testin.indexer.ProjectIndexer;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.TestRunItems;
import org.testin.services.Services;
import org.testin.testrun.RunEditorAttributes;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.RadioSelection;
import org.testin.ui.framework.SpellCheckedField;
import org.testin.ui.framework.TextArea;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

public final class FailureFields {
    private final @NotNull ComponentDialogBase<SpellCheckedField> actualResult;
    private final @NotNull ComponentDialogBase<RadioSelection<BugSeverity>> severity;
    private final @NotNull ComponentDialogBase<RadioSelection<BugPriority>> priority;
    private final @NotNull ComponentDialogBase<TextArea> errorCapture;

    private final @NotNull Map<byte[], String> named = new IdentityHashMap<>();

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-147
    public FailureFields(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunItems runItem) {
        final @NotNull List<byte[]> screenshots = Services.getInstance(p, ProjectIndexer.class).screenshots(runPath, runItem);
        IntStream.range(0, screenshots.size()).forEach(index -> named.put(screenshots.get(index), runItem.getScreenshots().get(index)));

        // Rule-EDITOR-PANEL-221
        actualResult = ComponentDialogBase.spellCheckedField(p, RunEditorAttributes.ACTUAL_RESULT.getName(), Bundle.message("dialog.failure.placeholder.actual"), runItem.getActualResult());

        severity = ComponentDialogBase.<BugSeverity>radios(RunEditorAttributes.BUG_SEVERITY.getName())
                .options(BugSeverity.CHOICES, BugSeverity::getLabel)
                .select(BugSeverity.orDefault(runItem.getBugSeverity()))
                .build();

        priority = ComponentDialogBase.<BugPriority>radios(RunEditorAttributes.BUG_PRIORITY.getName())
                .options(BugPriority.CHOICES, BugPriority::getLabel)
                .select(BugPriority.orDefault(runItem.getBugPriority()))
                .build();

        errorCapture = ComponentDialogBase.textArea()
                .caption(RunEditorAttributes.STACKTRACE.getName())
                .placeholder(Bundle.message("dialog.failure.placeholder.error"))
                .value(runItem.getStacktrace())
                .rows(5)
                .images(screenshots)
                .build();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148
    public @NotNull List<? extends ComponentDialogBase<?>> components() {
        return List.of(actualResult, severity, priority, errorCapture);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145, Rule-EDITOR-PANEL-219
    public void applyTo(final @NotNull TestRunItems runItem, final @NotNull List<String> screenshots) {
        runItem.setActualResult(actualResult.getComponent().getText().trim());
        runItem.setBugSeverity(severity.getComponent().getSelected());
        runItem.setBugPriority(priority.getComponent().getSelected());
        runItem.setStacktrace(errorCapture.getComponent().getText().trim());
        runItem.setScreenshots(screenshots);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public @NotNull List<String> screenshotNames(final @NotNull Function<List<byte[]>, List<String>> store) {
        final @NotNull List<byte[]> screenshots = errorCapture.getComponent().getImages();
        final @NotNull List<byte[]> pasted = screenshots.stream().filter(png -> !named.containsKey(png)).toList();
        final @NotNull List<String> names = store.apply(pasted);
        IntStream.range(0, pasted.size()).forEach(index -> named.put(pasted.get(index), names.get(index)));

        return screenshots.stream().map(named::get).toList();
    }

    public void onScreenshotsChanged(final @NotNull Runnable changed) {
        errorCapture.getComponent().onImagesChanged(changed);
    }

    public @NotNull SpellCheckedField firstField() {
        return actualResult.getComponent();
    }
}
