package org.testin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
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
import org.testin.setting.AppSettingsState;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Display;

import java.nio.file.Path;
import java.time.Duration;
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
        item.ifPresent(runItem ->
                runItem.recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName));

        Logger.trace("[RunStatusService]: Execution status updated -> " + currentTc.getDescription() + " = " + status);

        persistRun(p, editor);
        triggerFilterRefresh(editor);

        // Only when a verdict was actually recorded: a missing run item leaves
        // the status exactly as it was.
        item.ifPresent(recorded -> confirmVerdict(p, status, 1));

        ApplicationManager.getApplication().invokeLater(() -> {
            final @NotNull UUID currentId = currentTc.getId();
            final boolean stillInList = editor.getCurrentTestCases().stream()
                    .anyMatch(t -> t.getId().equals(currentId));
            final int nextIndex = stillInList ? executingIndex + 1 : executingIndex;
            editor.startTimerForIndex(nextIndex);
        });
    }

    public void executeManual(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull TestCaseDto tc, final @NotNull TestStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        // Stopping is the editor's own business - it owns the executing index -
        // and it happens before the verdict, so the case being judged is no
        // longer the one being timed.
        final int tcIndex = editor.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
            editor.stopExecution();
        }

        if (!recordVerdict(p, editor.getParent().getPath(), tc.getId(), status, duration, failure)) return;

        triggerFilterRefresh(editor);
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

        final @NotNull Optional<TestRunItems> found = run.getResults().stream()
                .filter(item -> item.getId().equals(caseId))
                .findFirst();

        if (found.isEmpty()) {
            Logger.warn("[RunStatusService]: '" + runPath.getFileName() + "' does not cover " + caseId + " - verdict not recorded");
            return false;
        }

        final @NotNull TestRunItems item = found.get();
        if (item.isRemoved()) {
            refuseRemoved(p);
            return false;
        }

        // Before the verdict, not after: passing clears everything a failure
        // described, so a message written afterward would survive onto a case
        // that passed. Written first, the verdict decides whether it stays.
        item.recordDuration(duration);
        failure.recordOn(item);
        item.recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName);

        Logger.trace("[RunStatusService]: Status updated -> " + caseId + " = " + status);

        Services.getInstance(p, ProjectIndexer.class).persistRun(runPath, run);

        confirmVerdict(p, status, 1);
        return true;
    }

    /**
     * Says why nothing happened, once, wherever a removed row was asked to take
     * something new - a verdict, a failure detail, an actual result. One
     * sentence for one situation, so the three surfaces that can ask do not each
     * word it differently.
     */
    public void refuseRemoved(final @NotNull Project p) {
        Services.getInstance(p, Notifier.class).softRefuse(p, "The test case was removed - the run keeps what it recorded");
    }

    public void applyStatus(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        final @NotNull List<TestCaseDto> selectedItems = list.getSelectedValuesList();
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
                .flatMap(item -> item.wouldClear(status).stream())
                .distinct()
                .toList();
    }

    /**
     * The sentence the tester reads before a verdict throws work away. Written
     * about what they typed rather than about fields: "the actual result", not
     * "actualResult will be reset".
     */
    private @NotNull String erasureWarning(final @NotNull List<String> losing, final int rows) {
        final @NotNull String where = rows == 1 ? "this case" : "these " + rows + " cases";

        return "Passing " + where + " clears " + Display.andJoin(losing)
                + ", because a case that passed has nothing to explain. "
                + "There is no copy of it anywhere else.";
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
                executeManual(p, editor, tc, status, Duration.ZERO, Failure.NONE);
            }
        } else {
            int recorded = 0;

            for (final TestCaseDto tc : selectedItems) {
                final @NotNull Optional<TestRunItems> found = editor.runItem(tc.getId())
                        .filter(item -> !item.isRemoved());

                if (found.isPresent()) {
                    found.get().recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName);
                    recorded++;

                    final int tcIndex = editor.getCurrentTestCases().indexOf(tc);
                    if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
                        editor.stopExecution();
                    }
                }
            }

            persistRun(p, editor);
            triggerFilterRefresh(editor);

            confirmVerdict(p, status, recorded);
        }
    }

    /**
     * The verdict is its own confirmation: "Passed" for one, "Passed 4" for a
     * selection. Once per gesture — the single-selection branch of
     * {@link #applyStatus} routes through {@link #executeNext} or
     * {@link #executeManual}, which confirm for themselves (#62).
     */
    private void confirmVerdict(final @NotNull Project p, final @NotNull TestStatus status, final int count) {
        if (count == 0) return;

        final @NotNull String label = status.getLabel();
        Services.getInstance(p, Notifier.class).softShow(p, count == 1 ? label : label + " " + count);
    }

    /**
     * Persistence goes through the indexer — the single owner of file access
     * (see CLAUDE.md). The indexer snapshots on this thread and writes through
     * its sequential run writer.
     */
    public void persistRun(final @NotNull Project p, final @NotNull RunEditor editor) {
        editor.run().ifPresent(tr ->
                Services.getInstance(p, ProjectIndexer.class).persistRun(editor.getParent().getPath(), tr));
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
        final @NotNull TestRunDto tr = Services.getInstance(p, ProjectIndexer.class).getTestRunByPath(runPath);

        int closed = 0;
        for (final TestRunItems item : tr.getResults()) {
            if (item.getStatus() == TestStatus.PENDING) {
                item.setStatus(TestStatus.UNTESTED);
                closed++;
            }
        }

        tr.markExecutionEnded();
        Services.getInstance(p, ProjectIndexer.class).persistRun(runPath, tr);

        if (closed > 0)
            Logger.info("Run finished with " + closed + " case(s) not executed; marked untested: " + runPath);
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
