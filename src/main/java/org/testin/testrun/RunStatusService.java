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
import org.testin.editor.TestinEditor;
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.Failure;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testrun.create.FailureFields;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunStatusService {

    /**
     * Judges the case the editor is executing, and moves on to the next.
     * <p>
     * No list, though it used to take one to repaint: the refresh that follows
     * rebuilds the page model, which repaints for itself. Without that
     * parameter the call says what it needs - a run being executed and a verdict
     * - so light mode records exactly as the grid does rather than through a
     * path of its own (#13).
     */
    public void executeNext(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull TestStatus status) {

        final int executingIndex = editor.getCurrentlyExecutingIndex();
        if (executingIndex == -1) return;

        final @NotNull TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);
        final @NotNull Optional<TestRunItems> item = editor.runItem(currentTc.getId());

        // As a change on the run the indexer holds, so a verdict given while a
        // sync brings the run in waits for it and lands on the run that arrived
        // (#66, finding 152).
        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        Services.getInstance(p, ProjectIndexer.class).changeRun(editor.getParent().getPath(),
                run -> run.resultOf(currentTc.getId()).filter(runItem -> !runItem.isRemoved()).ifPresent(runItem -> runItem.recordVerdict(status, tester)));

        Logger.trace("[RunStatusService]: Execution status updated -> " + currentTc.getDescription() + " = " + status);

        triggerFilterRefresh(editor);

        // Only when a verdict was actually recorded: a missing run item leaves
        // the status exactly as it was.
        item.ifPresent(recorded -> confirmVerdict(p, status, 1));

        // From the row just judged, not from the one after it. The editor moves
        // on to the next case still waiting for a verdict, so this no longer has
        // to know whether the filter refresh above dropped the judged case out of
        // the list and shifted everything up by one - both answers lead to the
        // same place (Rule-EDITOR-PANEL-130).
        ApplicationManager.getApplication().invokeLater(() -> editor.startTimerForIndex(executingIndex));
    }

    /**
     * Records one verdict and answers whether it landed. The caller confirms,
     * because only the caller knows whether this was one press or one of fifty
     * results an automated run is still reporting (#219).
     */
    public boolean executeManual(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull TestCaseDto tc, final @NotNull TestStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        // Stopping is the editor's own business - it owns the executing index -
        // and it happens before the verdict, so the case being judged is no
        // longer the one being timed.
        final int tcIndex = editor.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
            editor.stopExecution();
        }

        if (!recordVerdict(p, editor.getParent().getPath(), tc.getId(), status, duration, failure)) return false;

        triggerFilterRefresh(editor);
        return true;
    }

    /**
     * Records a verdict on one case of one run, with no editor in sight.
     * <p>
     * The recording never needed one. What {@link #executeManual} needs the
     * editor for is stopping the execution it is timing and refreshing the grid
     * it is drawing; everything between those two - find the item, write the
     * duration, the failure and the verdict, persist, and tell the tester - is
     * about the run and not about who is looking at it. So it is here, and the
     * editor is one caller rather than the shape of the call (#13).
     * <p>
     * <b>Both views work on one object.</b> {@code getTestRunByPath} hands back
     * the instance the indexer holds, and the run editor takes that same
     * instance into its own field - so a verdict recorded here is already
     * recorded in the grid behind it, with nothing to copy across and nothing
     * that can disagree.
     * <p>
     * Reports whether it happened. A case the run does not hold, or one already
     * removed, is refused rather than silently ignored: the caller has just told
     * a tester something was about to be recorded.
     */
    public boolean recordVerdict(final @NotNull Project p, final @NotNull Path runPath, final @NotNull UUID caseId, final @NotNull TestStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        final @NotNull TestRunDto run = Services.getInstance(p, ProjectIndexer.class).getTestRunByPath(runPath);

        if (liveItem(p, run, runPath, caseId).isEmpty()) return false;

        // Through the indexer rather than on the run read above: while a sync is
        // bringing this run's files in, the change waits for them and lands on
        // the run that arrived (#66, finding 129).
        Services.getInstance(p, ProjectIndexer.class).changeRun(runPath, current -> current.resultOf(caseId).ifPresentOrElse(item -> {
            // Before the verdict, not after: passing clears everything a failure
            // described, so a message written afterward would survive onto a case
            // that passed. Written first, the verdict decides whether it stays.
            item.recordDuration(duration);
            failure.recordOn(item);
            item.recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName);
        }, () -> Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' no longer covers " + caseId + " - verdict not recorded")));

        Logger.trace("[RunStatusService]: Status updated -> " + caseId + " = " + status);

        return true;
    }

    /**
     * UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167.
     * <p>
     * Records what a tester wrote about a failure on one case of one run, and
     * reports whether it landed.
     * <p>
     * On the run the indexer holds now, as {@link #recordVerdict} does, and not
     * on the run the editor holds. A sync that brings this run in replaces it in
     * the index while the editor still shows the old one, so persisting the
     * editor's run put the run back as it was before the sync (#66, finding 131).
     * A run the sync took away is not there to write, and the log says so.
     */
    public boolean recordFailureDetails(final @NotNull Project p, final @NotNull Path runPath, final @NotNull UUID caseId, final @NotNull FailureFields fields) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull Optional<TestRunDto> run = indexer.findTestRun(runPath);
        if (run.isEmpty()) {
            Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' is no longer indexed - failure details not recorded");
            return false;
        }

        if (liveItem(p, run.orElseThrow(), runPath, caseId).isEmpty()) return false;

        // The pasted screenshots first, as files beside the run, so the names
        // written next never point at a picture that has not landed (#313).
        final @NotNull List<String> screenshots = fields.screenshotNames(pasted -> indexer.storeScreenshots(runPath, pasted));

        // Through the indexer, as a verdict is, so details saved while a sync is
        // bringing this run in land on the run that arrived (#66, finding 129).
        indexer.changeRun(runPath, current -> current.resultOf(caseId).ifPresentOrElse(item -> fields.applyTo(item, screenshots),
                () -> Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' no longer covers " + caseId + " - failure details not recorded")));

        return true;
    }

    /**
     * The run's row for one case, when it can still take something new. A case
     * the run does not cover goes to the log; a removed one is refused to the
     * tester, because they just asked for something to be recorded on it.
     */
    private @NotNull Optional<TestRunItems> liveItem(final @NotNull Project p, final @NotNull TestRunDto run, final @NotNull Path runPath, final @NotNull UUID caseId) {
        final @NotNull Optional<TestRunItems> found = run.resultOf(caseId);

        if (found.isEmpty()) {
            Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' does not cover " + caseId + " - nothing recorded");
            return Optional.empty();
        }

        if (found.orElseThrow().isRemoved()) {
            refuseRemoved(p);
            return Optional.empty();
        }

        return found;
    }

    /**
     * Says why nothing happened, once, wherever a removed row was asked to take
     * something new - a verdict, a failure detail, an actual result. One
     * sentence for one situation, so the three surfaces that can ask do not each
     * word it differently.
     */
    public void refuseRemoved(final @NotNull Project p) {
        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.status.case.removed"));
    }

    public void applyStatus(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull List<TestCaseDto> selectedItems, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        if (selectedItems.isEmpty()) return;

        final @NotNull List<String> losing = wouldBeErased(editor, selectedItems, status);
        if (losing.isEmpty()) {
            record(p, status, editor, selectedItems);
            return;
        }

        new ConfirmDialog(p, status.getLabel(), erasureWarning(losing, selectedItems.size()), "", "",
                status.getLabel(), () -> record(p, status, editor, selectedItems)).show();
    }

    /**
     * Everything the selection holds that this verdict would erase, named once
     * each however many rows carry it.
     * <p>
     * Asked of the whole selection rather than row by row, because the tester is
     * answering one question about one gesture: eight rows failing eight
     * different ways is still "you typed things here and they are about to go".
     */
    private @NotNull List<String> wouldBeErased(final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> selected, final @NotNull TestStatus status) {
        return selected.stream()
                .map(tc -> editor.runItem(tc.getId()))
                .flatMap(Optional::stream)
                .filter(item -> !item.isRemoved())
                .flatMap(item -> item.wouldClear(status, Failure.NONE).stream())
                .distinct()
                .toList();
    }

    /**
     * The sentence the tester reads before a verdict throws work away. Written
     * about what they typed rather than about fields: "the actual result", not
     * "actualResult will be reset".
     */
    private @NotNull String erasureWarning(final @NotNull List<String> losing, final int rows) {
        final @NotNull String where = rows == 1
                ? Bundle.message("run.status.this.case")
                : Bundle.message("run.status.these.cases", String.valueOf(rows));

        return Bundle.message("run.status.passing.clears", where, Display.andJoin(losing));
    }

    /**
     * What {@link #applyStatus} does once the tester has nothing left to lose by
     * it - either because the verdict erases nothing, or because they said so.
     */
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
                if (executeManual(p, editor, tc, status, Duration.ZERO, Failure.NONE)) confirmVerdict(p, status, 1);
            }
        } else {
            final @NotNull List<UUID> judged = new ArrayList<>();

            for (final TestCaseDto tc : selectedItems) {
                if (editor.runItem(tc.getId()).filter(item -> !item.isRemoved()).isEmpty()) continue;

                judged.add(tc.getId());

                final int tcIndex = editor.getCurrentTestCases().indexOf(tc);
                if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
                    editor.stopExecution();
                }
            }

            // One change on the run the indexer holds, so verdicts given while a
            // sync brings the run in wait for it and land on the run that arrived
            // (#66, finding 152).
            final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
            Services.getInstance(p, ProjectIndexer.class).changeRun(editor.getParent().getPath(), run -> judged.forEach(id ->
                    run.resultOf(id).filter(item -> !item.isRemoved()).ifPresent(item -> item.recordVerdict(status, tester))));
            triggerFilterRefresh(editor);

            confirmVerdict(p, status, judged.size());
        }

        // Whichever way the last verdict arrived. Only a walk reaching its end
        // and an automated verdict used to ask, so judging the last pending case
        // from the menu or the keyboard left the run In Progress - still offering
        // to start something that had already happened (#217).
        editor.finishIfEverythingIsJudged();
    }

    /**
     * The verdict is its own confirmation: "Passed" for one, "Passed 4" for a
     * selection. Once per gesture — the single-selection branch of
     * {@link #applyStatus} routes through {@link #executeNext} or
     * {@link #executeManual}, which confirm for themselves (#62).
     * <p>
     * Counted by {@code softShowCounted} rather than here. Recording verdicts is
     * this class's job; deciding that four of them read "Passed 4" and one reads
     * "Passed" is the notifier's, and it was written out a second time here -
     * so a change to how a bulk confirmation counts would have landed on every
     * surface except this one (#291).
     */
    private void confirmVerdict(final @NotNull Project p, final @NotNull TestStatus status, final int count) {
        Services.getInstance(p, Notifier.class).softShowCounted(p, status.getLabel(), count);
    }

    /**
     * Writes what an open editor changed on its run - a stamp, a duration ticked
     * so far - through the indexer, the single owner of file access.
     * <p>
     * The editor's run is the object the index holds, so its change is already on
     * the run that is written. Written straight from the editor, a save made
     * while a sync was bringing the run in put the editor's run, the one from
     * before the sync, back over the run that arrived (#66, finding 152).
     */
    public void persistRun(final @NotNull Project p, final @NotNull RunEditor editor) {
        Services.getInstance(p, ProjectIndexer.class).saveRun(editor.getParent().getPath());
    }

    /**
     * Single source of truth for the run marker: always updates the
     * indexer-owned directory DTO (callers may hold another instance of the
     * same run), then persists through the indexer.
     * <p>
     * A status change is a modification, so it is recorded as one. This used to
     * take the change time as a parameter and write it over the marker's
     * createdAt - a leftover from when the reports read that field as the
     * execution date, which is the bug #27 exists to fix. Setting a run's status
     * from the tree therefore destroyed the run's creation time, and the editor
     * path had to pass the marker's own createdAt back in to defend against it.
     * Neither does anything now: createdAt means what it says, and touch records
     * who changed the status and when (#27).
     */
    public void persistMarker(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunStatus status) {
        final @NotNull TestRunDirectoryDto trd = Services.getInstance(p, ProjectIndexer.class).getTestRunDirByPath(runPath);

        final @NotNull TestRunMarker marker = trd.getMarker();
        marker.setStatus(status);
        marker.touch(Services.getInstance(p, AppSettingsState.class).testerName);

        if (status.isTerminal()) finishRun(p, runPath);

        Services.getInstance(p, ProjectIndexer.class).persistRunMarker(runPath, marker);
    }

    /**
     * A finished run has nothing pending and, if it was ever started, has ended:
     * everything not executed by the time it completes or closes is untested, and
     * the plugin says so rather than leaving a case queued for a run that will
     * never take it; and the run's execution end is stamped, so a report on a run
     * closed from the tree does not say it never ended.
     * <p>
     * Here rather than in the action that closes the run, because two of them do:
     * the run editor's own status change and the tree's Set Status menu. Only the
     * first used to convert, so closing a run from the tree left its pending cases
     * pending forever, and every report counted them under a heading that said the
     * run had outstanding work.
     * <p>
     * The case status is the only thing set on the cases. Stamping executedAt and
     * executedBy here — which the editor's version did — recorded the person who
     * closed the run as having executed cases nobody ran, and put their name in
     * the report's Executed By line. When the run closed is on the run's own marker.
     */
    private void finishRun(final @NotNull Project p, final @NotNull Path runPath) {
        // As a change on the run the indexer holds, so a run completed while a
        // sync brings it in closes the run that arrived (#66, finding 152).
        Services.getInstance(p, ProjectIndexer.class).changeRun(runPath, tr -> {
            int closed = 0;
            for (final TestRunItems item : tr.getResults()) {
                // A removed item is left as its file holds it: deleting a test
                // case never changes a run item (#66, finding 110).
                if (item.shownStatus() == TestStatus.PENDING) {
                    item.setStatus(TestStatus.UNTESTED);
                    closed++;
                }
            }

            tr.markExecutionEnded();

            if (closed > 0)
                Logger.info("Run finished with " + closed + " case(s) not executed; marked untested: " + runPath);
        });
    }

    /**
     * Shows what the verdict did, on the EDT.
     * <p>
     * One overload, where there were two: the second took the list and repainted
     * it first, which the rebuild in {@link #refreshEditor} does again a line
     * later.
     */
    private void triggerFilterRefresh(final @NotNull TestinEditor editor) {
        ApplicationManager.getApplication().invokeLater(() -> refreshEditor(editor));
    }

    /**
     * What the refresh does to the editor itself, on the EDT.
     */
    private void refreshEditor(final @NotNull TestinEditor editor) {
        if (editor instanceof RunEditor runEditor) {
            runEditor.refreshAfterStatusChange();
        } else if (editor instanceof Toolbar toolbar) {
            toolbar.onToolBarFilterSelectionChanged();
        }
    }
}
