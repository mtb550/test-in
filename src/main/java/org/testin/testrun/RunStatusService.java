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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.indexer.TestRuns;
import org.testin.logger.Logger;
import org.testin.model.Failure;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testrun.failure.FailureFields;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunStatusService {
    private final @NotNull Project p;

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-238
    private static @NotNull TestCaseDto asItIsNow(final @NotNull TestRunItems item) {
        return item.liveTestCase().copy();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130
    public void executeNext(final @NotNull RunEditor editor, final @NotNull TestStatus status) {
        final int executingIndex = editor.getWalk().getCurrentlyExecutingIndex();
        if (executingIndex == -1) {
            // Rule-EDITOR-PANEL-227
            if (editor.getWalk().executingTestCaseIsHidden())
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.executing.hidden"));
            return;
        }

        final @NotNull TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        if (!recordOn(editor, currentTc.getId(), status, item -> item.recordVerdict(status, tester, asItIsNow(item))))
            return;

        confirmVerdict(status, 1);

        // Rule-EDITOR-PANEL-130
        ApplicationManager.getApplication().invokeLater(() -> editor.getWalk().startTimerForIndex(executingIndex));
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-241, Rule-EDITOR-PANEL-242
    public void recordReported(final @NotNull RunEditor editor, final @NotNull TestCaseDto tc, final @NotNull TestStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        final boolean clockCounted = editor.getWalk().clockIsOn(tc.getId());

        if (editor.getWalk().isExecuting(tc.getId())) {
            editor.getWalk().startTimerForIndex(editor.getWalk().getCurrentlyExecutingIndex() + 1);
        }

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        recordOn(editor, tc.getId(), status, item -> {
            if (!clockCounted) item.recordDuration(duration);
            failure.recordOn(item);
            item.recordVerdict(status, tester, asItIsNow(item));
        });
    }

    // UC-EDITOR-PANEL-038, Rule-EDITOR-PANEL-240
    private boolean correct(final @NotNull RunEditor editor, final @NotNull TestCaseDto tc, final @NotNull TestStatus status) {
        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        return recordOn(editor, tc.getId(), status, item -> item.correctVerdict(status, tester, asItIsNow(item)));
    }

    // Rule-EDITOR-PANEL-225
    private boolean recordOn(final @NotNull RunEditor editor, final @NotNull UUID testCaseId, final @NotNull TestStatus status, final @NotNull Consumer<TestRunItems> verdict) {
        final @NotNull Path runPath = editor.getParent().getPath();
        final @NotNull Optional<TestRunDto> held = heldRun(runPath);
        if (held.isEmpty() || liveItem(held.orElseThrow(), runPath, testCaseId).isEmpty()) return false;

        Services.getInstance(p, TestRuns.class).changeResult(runPath, testCaseId, verdict);

        Logger.trace("[RunStatusService]: Status updated -> " + testCaseId + " = " + status);

        triggerFilterRefresh(editor);
        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167, Rule-EDITOR-PANEL-256
    public boolean recordFailureDetails(final @NotNull Path runPath, final @NotNull UUID testCaseId, final @NotNull FailureFields fields) {
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        final @NotNull Optional<TestRunDto> run = heldRun(runPath);
        if (run.isEmpty()) return false;

        if (liveItem(run.orElseThrow(), runPath, testCaseId).isEmpty()) return false;

        fields.storePasted(pasted -> testRuns.storeScreenshots(runPath, pasted));

        testRuns.changeResult(runPath, testCaseId, fields::applyTo);

        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-225
    public @NotNull Optional<TestRunDto> heldRun(final @NotNull Path runPath) {
        final @NotNull Optional<TestRunDto> run = Services.getInstance(p, TestRuns.class).findTestRun(runPath);

        if (run.isEmpty()) {
            Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' is no longer indexed - nothing recorded");
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.run.gone"));
        }

        return run;
    }

    private @NotNull Optional<TestRunItems> liveItem(final @NotNull TestRunDto run, final @NotNull Path runPath, final @NotNull UUID testCaseId) {
        final @NotNull Optional<TestRunItems> found = run.resultOf(testCaseId);

        if (found.isEmpty()) {
            Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' does not cover " + testCaseId + " - nothing recorded");

            // Rule-EDITOR-PANEL-225
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.case.not.covered"));
            return Optional.empty();
        }

        if (found.orElseThrow().isRemoved()) {
            refuseRemoved();
            return Optional.empty();
        }

        return found;
    }

    public void refuseRemoved() {
        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.case.removed"));
    }

    public void applyStatus(final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selectedItems, final @NotNull TestStatus status) {
        if (selectedItems.isEmpty()) return;

        final @NotNull List<String> losing = wouldBeErased(editor, selectedItems, status);
        if (losing.isEmpty()) {
            record(status, editor, selectedItems);
            return;
        }

        new ConfirmDialog(p, status.getLabel(), erasureWarning(losing, selectedItems.size()), "", "",
                status.getLabel(), () -> record(status, editor, selectedItems)).show();
    }

    private @NotNull List<String> wouldBeErased(final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selected, final @NotNull TestStatus status) {
        return selected.stream()
                .map(tc -> editor.runItem(tc.getId()))
                .flatMap(Optional::stream)
                .filter(item -> !item.isRemoved())
                .flatMap(item -> item.wouldClear(status, Failure.NONE).stream())
                .distinct()
                .toList();
    }

    private @NotNull String erasureWarning(final @NotNull List<String> losing, final int rows) {
        final @NotNull String where = rows == 1
                ? Bundle.message("run.status.this.case")
                : Bundle.message("run.status.these.cases", String.valueOf(rows));

        return Bundle.message("run.status.passing.clears", where, Display.andJoin(losing));
    }

    private void record(final @NotNull TestStatus status, final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        if (selectedItems.size() == 1) recordOne(status, editor, selectedItems.getFirst());
        else recordMany(status, editor, selectedItems);
    }

    private void recordOne(final @NotNull TestStatus status, final @NotNull RunEditor editor, final @NotNull TestCaseDto tc) {
        if (editor.runItem(tc.getId()).filter(TestRunItems::isRemoved).isPresent()) {
            refuseRemoved();
            return;
        }

        if (editor.getWalk().isExecuting(tc.getId())) executeNext(editor, status);
        else if (correct(editor, tc, status)) confirmVerdict(status, 1);

        editor.getWalk().finishIfEverythingIsJudged();
    }

    private void recordMany(final @NotNull TestStatus status, final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        final @NotNull Optional<TestRunDto> held = heldRun(editor.getParent().getPath());
        if (held.isEmpty()) return;

        final @NotNull List<UUID> judged = new ArrayList<>();

        for (final TestCaseDto tc : selectedItems) {
            if (held.orElseThrow().resultOf(tc.getId()).filter(item -> !item.isRemoved()).isEmpty()) continue;

            judged.add(tc.getId());

            if (editor.getWalk().isExecuting(tc.getId())) editor.getWalk().stopExecutionUntimed();
        }

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        judged.forEach(id -> testRuns.changeResult(editor.getParent().getPath(), id, item -> item.correctVerdict(status, tester, asItIsNow(item))));
        triggerFilterRefresh(editor);

        confirmVerdict(status, judged.size());

        editor.getWalk().finishIfEverythingIsJudged();
    }

    private void confirmVerdict(final @NotNull TestStatus status, final int count) {
        Services.getInstance(p, Notifier.class).softShowCounted(p, status.getLabel(), count);
    }

    private void triggerFilterRefresh(final @NotNull RunEditor editor) {
        ApplicationManager.getApplication().invokeLater(editor::refreshView);
    }
}
