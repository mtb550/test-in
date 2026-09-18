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

package org.testin.testrun.create;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;
import org.testin.testrun.RunEditorAttributes;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.TestRunItems;
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

/**
 * The four things a tester writes down about a failure: what actually happened,
 * how bad it is, how urgent it is, and the error itself.
 * <p>
 * <b>One declaration, two places to fill it in.</b> {@link FailedResultDialog}
 * asks for them in the run editor; light mode asks for them inside its own
 * window, because that dialog is modal and owned by the IDE frame, so opening
 * it would raise IntelliJ and put the tester back exactly where light mode
 * exists to keep them out of (#13). A failure recorded in one place and a
 * failure recorded in the other have to be the same record rather than two
 * shapes of one - so the fields, their defaults, their wording and the order
 * they are written back in are declared here and nowhere else.
 * <p>
 * Built against one run row and holding it: these are that row's values, and a
 * second row's would need a second set of fields.
 */
public final class FailureFields {

    private final @NotNull ComponentDialogBase<SpellCheckedField> actualResult;
    private final @NotNull ComponentDialogBase<RadioSelection<BugSeverity>> severity;
    private final @NotNull ComponentDialogBase<RadioSelection<BugPriority>> priority;
    private final @NotNull ComponentDialogBase<TextArea> errorCapture;

    /**
     * Each screenshot under the error box that has a file, and that file's name.
     * By identity, so a save names a stored screenshot as it was named rather
     * than storing it a second time under a new one.
     */
    private final @NotNull Map<byte[], String> named = new IdentityHashMap<>();

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-147
    public FailureFields(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunItems runItem) {
        final @NotNull List<byte[]> screenshots = Services.getInstance(p, ProjectIndexer.class).screenshots(runPath, runItem);
        IntStream.range(0, screenshots.size()).forEach(index -> named.put(screenshots.get(index), runItem.getScreenshots().get(index)));

        // Rule-EDITOR-PANEL-221: spell checked, as every field of a form that
        // describes a test case is (#314).
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

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148.
     * <p>
     * The four, in the order a tester fills them in: what happened, then how
     * much it matters, then the evidence.
     */
    public @NotNull List<? extends ComponentDialogBase<?>> components() {
        return List.of(actualResult, severity, priority, errorCapture);
    }

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145, Rule-EDITOR-PANEL-219.
     * <p>
     * Writes what was typed onto the run row, and the names of the screenshots,
     * which the indexer has already kept as files beside the run (#313).
     * <p>
     * Only ever called by a save. Escape must never commit an edit, so nothing
     * here happens as the tester types.
     */
    public void applyTo(final @NotNull TestRunItems runItem, final @NotNull List<String> screenshots) {
        runItem.setActualResult(actualResult.getComponent().getText().trim());
        runItem.setBugSeverity(severity.getComponent().getSelected());
        runItem.setBugPriority(priority.getComponent().getSelected());
        runItem.setStacktrace(errorCapture.getComponent().getText().trim());
        runItem.setScreenshots(screenshots);
    }

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219.
     * <p>
     * The file names of the screenshots under the error box, in the order they
     * were pasted. One that has a file keeps its name; the ones pasted since are
     * handed to {@code store}, which keeps them as files and answers their names.
     */
    public @NotNull List<String> screenshotNames(final @NotNull Function<List<byte[]>, List<String>> store) {
        final @NotNull List<byte[]> screenshots = errorCapture.getComponent().getImages();
        final @NotNull List<byte[]> pasted = screenshots.stream().filter(png -> !named.containsKey(png)).toList();
        final @NotNull List<String> names = store.apply(pasted);
        IntStream.range(0, pasted.size()).forEach(index -> named.put(pasted.get(index), names.get(index)));

        return screenshots.stream().map(named::get).toList();
    }

    /**
     * Runs after a screenshot is pasted under the error box or taken out, which
     * changes how tall the box is.
     */
    public void onScreenshotsChanged(final @NotNull Runnable changed) {
        errorCapture.getComponent().onImagesChanged(changed);
    }

    /**
     * Where the tester starts typing.
     */
    public @NotNull SpellCheckedField firstField() {
        return actualResult.getComponent();
    }
}
