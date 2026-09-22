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
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.Failure;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.TestCaseSnapshot;
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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunStatusService {
    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-238
    private static @NotNull TestCaseDto asItIsNow(final @NotNull Project p, final @NotNull TestRunItems item) {
        return TestCaseSnapshot.copy(p, item.liveTestCase());
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130
    public void executeNext(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull TestStatus status) {
        final int executingIndex = editor.getCurrentlyExecutingIndex();
        if (executingIndex == -1) {
            // Rule-EDITOR-PANEL-227
            if (editor.executingTestCaseIsHidden())
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.executing.hidden"));
            return;
        }

        final @NotNull TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        if (!recordOn(p, editor, currentTc.getId(), status, item -> item.recordVerdict(status, tester, asItIsNow(p, item))))
            return;

        confirmVerdict(p, status, 1);

        // Rule-EDITOR-PANEL-130
        ApplicationManager.getApplication().invokeLater(() -> editor.startTimerForIndex(executingIndex));
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-241, Rule-EDITOR-PANEL-242
    public void recordReported(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull TestCaseDto tc, final @NotNull TestStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        final int tcIndex = editor.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
            editor.startTimerForIndex(tcIndex + 1);
        }

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        recordOn(p, editor, tc.getId(), status, item -> {
            item.recordDuration(duration);
            failure.recordOn(item);
            item.recordVerdict(status, tester, asItIsNow(p, item));
        });
    }

    // UC-EDITOR-PANEL-038, Rule-EDITOR-PANEL-240
    private boolean correct(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull TestCaseDto tc, final @NotNull TestStatus status) {
        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        return recordOn(p, editor, tc.getId(), status, item -> item.correctVerdict(status, tester, asItIsNow(p, item)));
    }

    // Rule-EDITOR-PANEL-225
    private boolean recordOn(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull UUID testCaseId, final @NotNull TestStatus status, final @NotNull Consumer<TestRunItems> verdict) {
        final @NotNull Path runPath = editor.getParent().getPath();
        final @NotNull Optional<TestRunDto> held = heldRun(p, runPath);
        if (held.isEmpty() || liveItem(p, held.orElseThrow(), runPath, testCaseId).isEmpty()) return false;

        Services.getInstance(p, ProjectIndexer.class).changeRun(runPath, current -> current.resultOf(testCaseId).ifPresentOrElse(verdict,
                () -> Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' no longer covers " + testCaseId + " - verdict not recorded")));

        Logger.trace("[RunStatusService]: Status updated -> " + testCaseId + " = " + status);

        triggerFilterRefresh(editor);
        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167
    public boolean recordFailureDetails(final @NotNull Project p, final @NotNull Path runPath, final @NotNull UUID testCaseId, final @NotNull FailureFields fields) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull Optional<TestRunDto> run = heldRun(p, runPath);
        if (run.isEmpty()) return false;

        if (liveItem(p, run.orElseThrow(), runPath, testCaseId).isEmpty()) return false;

        final @NotNull List<String> screenshots = fields.screenshotNames(pasted -> indexer.storeScreenshots(runPath, pasted));

        indexer.changeRun(runPath, current -> current.resultOf(testCaseId).ifPresentOrElse(item -> fields.applyTo(item, screenshots),
                () -> Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' no longer covers " + testCaseId + " - failure details not recorded")));

        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-225
    public @NotNull Optional<TestRunDto> heldRun(final @NotNull Project p, final @NotNull Path runPath) {
        final @NotNull Optional<TestRunDto> run = Services.getInstance(p, ProjectIndexer.class).findTestRun(runPath);

        if (run.isEmpty()) {
            Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' is no longer indexed - nothing recorded");
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.run.gone"));
        }

        return run;
    }

    private @NotNull Optional<TestRunItems> liveItem(final @NotNull Project p, final @NotNull TestRunDto run, final @NotNull Path runPath, final @NotNull UUID testCaseId) {
        final @NotNull Optional<TestRunItems> found = run.resultOf(testCaseId);

        if (found.isEmpty()) {
            Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' does not cover " + testCaseId + " - nothing recorded");

            // Rule-EDITOR-PANEL-225
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.case.not.covered"));
            return Optional.empty();
        }

        if (found.orElseThrow().isRemoved()) {
            refuseRemoved(p);
            return Optional.empty();
        }

        return found;
    }

    public void refuseRemoved(final @NotNull Project p) {
        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.case.removed"));
    }

    public void applyStatus(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selectedItems, final @NotNull TestStatus status) {
        if (selectedItems.isEmpty()) return;

        final @NotNull List<String> losing = wouldBeErased(editor, selectedItems, status);
        if (losing.isEmpty()) {
            record(p, status, editor, selectedItems);
            return;
        }

        new ConfirmDialog(p, status.getLabel(), erasureWarning(losing, selectedItems.size()), "", "",
                status.getLabel(), () -> record(p, status, editor, selectedItems)).show();
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

    private void record(final @NotNull Project p, final @NotNull TestStatus status, final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        if (selectedItems.size() == 1) {
            final @NotNull TestCaseDto tc = selectedItems.getFirst();
            if (editor.runItem(tc.getId()).filter(TestRunItems::isRemoved).isPresent()) {
                refuseRemoved(p);
                return;
            }

            final int globalIndex = editor.getCurrentTestCases().indexOf(tc);
            if (globalIndex == editor.getCurrentlyExecutingIndex()) {
                executeNext(p, editor, status);
            } else {
                if (correct(p, editor, tc, status)) confirmVerdict(p, status, 1);
            }
        } else {
            final @NotNull Optional<TestRunDto> held = heldRun(p, editor.getParent().getPath());
            if (held.isEmpty()) return;

            final @NotNull List<UUID> judged = new ArrayList<>();

            for (final TestCaseDto tc : selectedItems) {
                if (held.orElseThrow().resultOf(tc.getId()).filter(item -> !item.isRemoved()).isEmpty()) continue;

                judged.add(tc.getId());

                final int tcIndex = editor.getCurrentTestCases().indexOf(tc);
                if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
                    editor.stopExecutionUntimed();
                }
            }

            final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
            Services.getInstance(p, ProjectIndexer.class).changeRun(editor.getParent().getPath(), run -> judged.forEach(id ->
                    run.resultOf(id).filter(item -> !item.isRemoved()).ifPresent(item -> item.correctVerdict(status, tester, asItIsNow(p, item)))));
            triggerFilterRefresh(editor);

            confirmVerdict(p, status, judged.size());
        }

        editor.finishIfEverythingIsJudged();
    }

    private void confirmVerdict(final @NotNull Project p, final @NotNull TestStatus status, final int count) {
        Services.getInstance(p, Notifier.class).softShowCounted(p, status.getLabel(), count);
    }

    private void triggerFilterRefresh(final @NotNull RunEditor editor) {
        ApplicationManager.getApplication().invokeLater(editor::refreshView);
    }
}
