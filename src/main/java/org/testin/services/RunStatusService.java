package org.testin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.TestinEditor;
import org.testin.editor.runEditor.RunEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.enums.TestRunStatus;
import org.testin.enums.TestStatus;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.setting.AppSettingsState;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunStatusService {

    public void executeNext(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        final int executingIndex = editor.getCurrentlyExecutingIndex();
        if (executingIndex == -1) return;

        final TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);
        final TestRunItems item = editor.getResultsMap().get(currentTc.getId());

        if (item != null) {
            item.recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName);
        }

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

        final TestRunItems item = editor.getResultsMap().get(tc.getId());
        if (item == null) return;

        final int tcIndex = editor.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
            editor.stopExecution();
        }

        item.recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName);

        Logger.trace("[RunStatusService]: Status updated -> " + tc.getDescription() + " = " + status);

        persistRun(p, editor);
        triggerFilterRefresh(ui, null);

        confirmVerdict(p, status, 1);
    }

    public void applyStatus(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        final List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        if (selectedItems.size() == 1) {
            final TestCaseDto tc = selectedItems.getFirst();
            final int globalIndex = editor.getCurrentTestCases().indexOf(tc);
            if (globalIndex == editor.getCurrentlyExecutingIndex()) {
                executeNext(p, ui, list, status);
            } else {
                executeManual(p, ui, tc, status);
            }
        } else {
            int recorded = 0;

            for (final TestCaseDto tc : selectedItems) {
                final TestRunItems item = editor.getResultsMap().get(tc.getId());
                if (item != null) {
                    item.recordVerdict(status, Services.getInstance(p, AppSettingsState.class).testerName);
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
        final TestRunDto tr = editor.getTr();
        if (tr == null) return;

        Services.getInstance(p, ProjectIndexer.class).persistRun(editor.getParent().getPath(), tr);
    }

    /**
     * Single source of truth for the run marker: always updates the
     * indexer-owned directory DTO (callers may hold another instance of the
     * same run), then persists through the indexer.
     */
    public void persistMarker(final @NotNull Project p, final @NotNull Path runPath,
                              final @NotNull TestRunStatus status, final @Nullable ZonedDateTime statusChangedAt) {
        final TestRunDirectoryDto trd = Services.getInstance(p, ProjectIndexer.class).getTestRunDirByPath(runPath);
        if (trd == null) {
            Logger.warn("persistMarker: run not indexed: " + runPath);
            return;
        }

        final TestRunMarker marker = trd.getMarker();
        marker.setStatus(status);
        if (statusChangedAt != null) marker.setCreatedAt(statusChangedAt);

        Services.getInstance(p, ProjectIndexer.class).persistRunMarker(runPath, marker);
    }

    private void triggerFilterRefresh(final @NotNull TestinEditor editor, final @Nullable JBList<TestCaseDto> list) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (list != null) {
                list.repaint();
            }
            if (editor instanceof RunEditor runEditor) {
                runEditor.refreshAfterStatusChange();
            } else if (editor instanceof Toolbar) {
                ((Toolbar) editor).onToolBarFilterSelectionChanged();
            }
        });
    }
}
