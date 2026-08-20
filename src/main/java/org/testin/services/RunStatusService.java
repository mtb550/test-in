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
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.setting.AppSettingsState;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunStatusService {

    public void executeNext(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        final int executingIndex = editor.getCurrentlyExecutingIndex();
        if (executingIndex == -1) return;

        final TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);
        final Optional<TestRunItems> item = editor.runItem(currentTc.getId());
        item.ifPresent(runItem ->
                runItem.recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName));

        Logger.trace("[RunStatusService]: Execution status updated -> " + currentTc.getDescription() + " = " + status);

        persistRun(p, editor);
        triggerFilterRefresh(ui, list);

        // Only when a verdict was actually recorded: a missing run item leaves
        // the status exactly as it was.
        if (item != null) confirmVerdict(p, status, 1);

        ApplicationManager.getApplication().invokeLater(() -> {
            final UUID currentId = currentTc.getId();
            final boolean stillInList = editor.getCurrentTestCases().stream()
                    .anyMatch(t -> t.getId().equals(currentId));
            final int nextIndex = stillInList ? executingIndex + 1 : executingIndex;
            editor.startTimerForIndex(nextIndex);
        });
    }

    public void executeManual(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull TestCaseDto tc, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        final Optional<TestRunItems> found = editor.runItem(tc.getId());
        if (found.isEmpty()) return;
        final TestRunItems item = found.get();

        final int tcIndex = editor.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
            editor.stopExecution();
        }

        item.recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName);

        Logger.trace("[RunStatusService]: Status updated -> " + tc.getDescription() + " = " + status);

        persistRun(p, editor);
        triggerFilterRefresh(ui);

        confirmVerdict(p, status, 1);
    }

    /**
     * Says why nothing happened, once, wherever a removed row was asked to take
     * something new - a verdict, a failure detail, an actual result. One
     * sentence for one situation, so the three surfaces that can ask do not each
     * word it differently.
     */
    public void refuseRemoved(final @NotNull Project p) {
        Services.getInstance(p, Notifier.class).softShow(p, "The test case was removed - the run keeps what it recorded");
    }

    public void applyStatus(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        final List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        if (selectedItems.size() == 1) {
            final TestCaseDto tc = selectedItems.getFirst();
            if (editor.runItem(tc.getId()).filter(TestRunItems::isRemoved).isPresent()) {
                refuseRemoved(p);
                return;
            }

            final int globalIndex = editor.getCurrentTestCases().indexOf(tc);
            if (globalIndex == editor.getCurrentlyExecutingIndex()) {
                executeNext(p, ui, list, status);
            } else {
                executeManual(p, ui, tc, status);
            }
        } else {
            int recorded = 0;

            for (final TestCaseDto tc : selectedItems) {
                final Optional<TestRunItems> found = editor.runItem(tc.getId())
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
            triggerFilterRefresh(ui, list);

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

        final String label = status.getLabel();
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
    public void persistMarker(final @NotNull Project p, final @NotNull Path runPath,
                              final @NotNull TestRunStatus status) {
        final TestRunDirectoryDto trd = Services.getInstance(p, ProjectIndexer.class).getTestRunDirByPath(runPath);

        final TestRunMarker marker = trd.getMarker();
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
        final TestRunDto tr = Services.getInstance(p, ProjectIndexer.class).getTestRunByPath(runPath);

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
     * The same refresh, for a caller with no list of its own to repaint.
     */
    private void triggerFilterRefresh(final @NotNull TestinEditor editor) {
        ApplicationManager.getApplication().invokeLater(() -> refreshEditor(editor));
    }

    private void triggerFilterRefresh(final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        ApplicationManager.getApplication().invokeLater(() -> {
            list.repaint();
            refreshEditor(editor);
        });
    }

    /**
     * What both refreshes do to the editor itself, on the EDT.
     */
    private void refreshEditor(final @NotNull TestinEditor editor) {
        if (editor instanceof RunEditor runEditor) {
            runEditor.refreshAfterStatusChange();
        } else if (editor instanceof Toolbar toolbar) {
            toolbar.onToolBarFilterSelectionChanged();
        }
    }
}
