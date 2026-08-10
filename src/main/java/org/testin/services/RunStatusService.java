package org.testin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.enums.DirectoryType;
import org.testin.enums.TestRunStatus;
import org.testin.enums.TestStatus;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.settings.AppSettingsState;
import org.testin.util.FilesUtil;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunStatusService {

    /**
     * All run-status disk writes go through this one sequential executor,
     * so writes can never interleave or race each other.
     */
    private final ExecutorService persistExecutor =
            AppExecutorUtil.createBoundedApplicationPoolExecutor("Testin Run Status Writer", 1);

    public void executeNext(final @NotNull Project p, final @NotNull IEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        int executingIndex = editor.getCurrentlyExecutingIndex();
        if (executingIndex == -1) return;

        TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);
        TestRunItems item = editor.getResultsMap().get(currentTc.getId());

        if (item != null) {
            item.setStatus(status);
            item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            item.setExecutedBy(Services.getInstance(p, AppSettingsState.class).testerName);
        }

        Logger.trace("[RunStatusService]: Execution status updated -> " + currentTc.getDescription() + " = " + status);

        persistRun(p, editor);
        triggerFilterRefresh(ui, list);

        ApplicationManager.getApplication().invokeLater(() -> {
            UUID currentId = currentTc.getId();
            boolean stillInList = editor.getCurrentTestCases().stream()
                    .anyMatch(t -> t.getId().equals(currentId));
            int nextIndex = stillInList ? executingIndex + 1 : executingIndex;
            editor.startTimerForIndex(nextIndex);
        });
    }

    public void executeManual(final @NotNull Project p, final @NotNull IEditor ui, final @NotNull TestCaseDto tc, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        TestRunItems item = editor.getResultsMap().get(tc.getId());
        if (item == null) return;

        int tcIndex = editor.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
            editor.stopExecution();
        }

        item.setStatus(status);
        item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        item.setExecutedBy(Services.getInstance(p, AppSettingsState.class).testerName);

        Logger.trace("[RunStatusService]: Status updated -> " + tc.getDescription() + " = " + status);

        persistRun(p, editor);
        triggerFilterRefresh(ui, null);
    }

    public void applyStatus(final @NotNull Project p, final @NotNull IEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        if (selectedItems.size() == 1) {
            TestCaseDto tc = selectedItems.getFirst();
            int globalIndex = editor.getCurrentTestCases().indexOf(tc);
            if (globalIndex == editor.getCurrentlyExecutingIndex()) {
                executeNext(p, ui, list, status);
            } else {
                executeManual(p, ui, tc, status);
            }
        } else {
            for (TestCaseDto tc : selectedItems) {
                TestRunItems item = editor.getResultsMap().get(tc.getId());
                if (item != null) {
                    item.setStatus(status);
                    item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                    item.setExecutedBy(Services.getInstance(p, AppSettingsState.class).testerName);

                    int tcIndex = editor.getCurrentTestCases().indexOf(tc);
                    if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
                        editor.stopExecution();
                    }
                }
            }

            persistRun(p, editor);
            triggerFilterRefresh(ui, list);
        }
    }

    /**
     * Single-writer persistence for run results: the JSON snapshot is taken on the
     * calling (EDT) thread — so it can never observe a half-applied mutation — and
     * the sequential writer performs only the disk I/O, in submission order.
     */
    public void persistRun(final @NotNull Project p, final @NotNull RunEditor editor) {
        final TestRunDto tr = editor.getTr();
        if (tr == null || editor.getParent() == null) return;

        final Path runPath = editor.getParent().getPath();
        final byte[] snapshot;
        try {
            snapshot = Services.getInstance(p, Mapper.class).writeValueAsBytes(tr);
        } catch (final Exception ex) {
            Logger.error("Failed to snapshot test run data: " + ex.getMessage());
            return;
        }

        persistExecutor.execute(() -> {
            try {
                Services.getInstance(p, ProjectIndexer.class).registerTestRun(runPath, tr);
                Services.getInstance(p, FilesUtil.class).write(p, runPath.resolve(runPath.getFileName() + ".json"), snapshot);
                Logger.trace("Run results persisted for " + runPath.getFileName());
            } catch (final Exception ex) {
                Logger.error("Failed to persist test run data: " + ex.getMessage());
            }
        });
    }

    /**
     * Single source of truth for the run marker: always updates the indexer-owned
     * directory DTO (callers may hold another instance of the same run), snapshots
     * it on the calling thread, and writes through the sequential writer.
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

        final byte[] snapshot;
        try {
            snapshot = Services.getInstance(p, Mapper.class).writeValueAsBytes(marker);
        } catch (final Exception ex) {
            Logger.error("Failed to snapshot run marker: " + ex.getMessage());
            return;
        }

        persistExecutor.execute(() -> {
            try {
                Services.getInstance(p, FilesUtil.class).write(p, runPath.resolve(DirectoryType.TR.getMarker()), snapshot);
                Logger.trace("Marker persisted -> " + status.getLabel());
            } catch (final Exception ex) {
                Logger.error("Failed to persist marker: " + ex.getMessage());
            }
        });
    }

    private void triggerFilterRefresh(final @NotNull IEditor editor, final JBList<TestCaseDto> list) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (list != null) {
                list.repaint();
            }
            if (editor instanceof RunEditor runEditor) {
                runEditor.refreshAfterStatusChange();
            } else if (editor instanceof IToolBar) {
                ((IToolBar) editor).onToolBarFilterSelectionChanged();
            }
        });
    }
}
