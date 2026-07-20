package org.testin.util.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunStatusService {

    public void executeNext(final @NotNull Project project, final @NotNull IEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        int executingIndex = editor.getCurrentlyExecutingIndex();
        if (executingIndex == -1) return;

        TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);
        TestRunItems item = editor.getResultsMap().get(currentTc.getId());

        if (item != null) {
            item.setStatus(status);
            item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        }

        Log.trace("[RunStatusService]: Execution status updated -> " + currentTc.getDescription() + " = " + status);

        persistRunDataAsync(project, editor);
        triggerFilterRefresh(ui, list);

        ApplicationManager.getApplication().invokeLater(() -> {
            UUID currentId = currentTc.getId();
            boolean stillInList = editor.getCurrentTestCases().stream()
                    .anyMatch(t -> t.getId().equals(currentId));
            int nextIndex = stillInList ? executingIndex + 1 : executingIndex;
            editor.startTimerForIndex(nextIndex);
        });
    }

    public void executeManual(final @NotNull Project project, final @NotNull IEditor ui, final @NotNull TestCaseDto tc, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        TestRunItems item = editor.getResultsMap().get(tc.getId());
        if (item == null) return;

        int tcIndex = editor.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
            editor.stopExecution();
        }

        item.setStatus(status);
        item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        Log.trace("[RunStatusService]: Status updated -> " + tc.getDescription() + " = " + status);

        persistRunDataAsync(project, editor);
        triggerFilterRefresh(ui, null);
    }

    public void applyStatus(final @NotNull Project project, final @NotNull IEditor ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditor editor)) return;

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        if (selectedItems.size() == 1) {
            TestCaseDto tc = selectedItems.getFirst();
            int globalIndex = editor.getCurrentTestCases().indexOf(tc);
            if (globalIndex == editor.getCurrentlyExecutingIndex()) {
                executeNext(project, ui, list, status);
            } else {
                executeManual(project, ui, tc, status);
            }
        } else {
            for (TestCaseDto tc : selectedItems) {
                TestRunItems item = editor.getResultsMap().get(tc.getId());
                if (item != null) {
                    item.setStatus(status);
                    item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

                    int tcIndex = editor.getCurrentTestCases().indexOf(tc);
                    if (tcIndex != -1 && tcIndex == editor.getCurrentlyExecutingIndex()) {
                        editor.stopExecution();
                    }
                }
            }

            persistRunDataAsync(project, editor);
            triggerFilterRefresh(ui, list);
        }
    }

    private void persistRunDataAsync(final @NotNull Project project, final @NotNull RunEditor editor) {
        if (editor.getTr() == null || editor.getParent() == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = editor.getParent().getPath();
                Services.getInstance(project, ProjectIndexer.class).putTestRun(dirPath, editor.getTr());
            } catch (final Exception ex) {
                Log.error("Failed to persist test run data: " + ex.getMessage());
            }
        });
    }

    private void triggerFilterRefresh(final @NotNull IEditor editor, final JBList<TestCaseDto> list) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (list != null) {
                list.repaint();
            }
            if (editor instanceof IToolBar) {
                ((IToolBar) editor).onToolBarFilterSelectionChanged();
            }
        });
    }
}
