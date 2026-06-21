package org.testin.util.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.runEditor.RunEditorUI;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.FilesUtil;
import org.testin.util.logger.Log;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunStatusService {

    public void executeNext(final @NotNull Project project, final @NotNull IEditorUI ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditorUI runUi)) return;

        int executingIndex = runUi.getCurrentlyExecutingIndex();
        if (executingIndex == -1) return;

        TestCaseDto currentTc = runUi.getCurrentTestCases().get(executingIndex);
        TestRunItems item = runUi.getResultsMap().get(currentTc.getId());

        if (item != null) {
            item.setStatus(status);
            item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        }

        Log.trace("[RunStatusService]: Execution status updated -> " + currentTc.getDescription() + " = " + status);

        persistRunDataAsync(project, runUi);
        triggerFilterRefresh(ui, list);

        ApplicationManager.getApplication().invokeLater(() -> {
            UUID currentId = currentTc.getId();
            boolean stillInList = runUi.getCurrentTestCases().stream()
                    .anyMatch(t -> t.getId().equals(currentId));
            int nextIndex = stillInList ? executingIndex + 1 : executingIndex;
            runUi.startTimerForIndex(nextIndex);
        });
    }

    public void executeManual(final @NotNull Project project, final @NotNull IEditorUI ui, final @NotNull TestCaseDto tc, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditorUI runUi)) return;

        TestRunItems item = runUi.getResultsMap().get(tc.getId());
        if (item == null) return;

        int tcIndex = runUi.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == runUi.getCurrentlyExecutingIndex()) {
            runUi.stopExecution();
        }

        item.setStatus(status);
        item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        Log.trace("[RunStatusService]: Status updated -> " + tc.getDescription() + " = " + status);

        persistRunDataAsync(project, runUi);
        triggerFilterRefresh(ui, null);
    }

    public void applyStatus(final @NotNull Project project, final @NotNull IEditorUI ui, final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status) {
        if (!(ui instanceof RunEditorUI runUi)) return;

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        if (selectedItems.size() == 1) {
            TestCaseDto tc = selectedItems.getFirst();
            int globalIndex = runUi.getCurrentTestCases().indexOf(tc);
            if (globalIndex == runUi.getCurrentlyExecutingIndex()) {
                executeNext(project, ui, list, status);
            } else {
                executeManual(project, ui, tc, status);
            }
        } else {
            for (TestCaseDto tc : selectedItems) {
                TestRunItems item = runUi.getResultsMap().get(tc.getId());
                if (item != null) {
                    item.setStatus(status);
                    item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

                    int tcIndex = runUi.getCurrentTestCases().indexOf(tc);
                    if (tcIndex != -1 && tcIndex == runUi.getCurrentlyExecutingIndex()) {
                        runUi.stopExecution();
                    }
                }
            }

            persistRunDataAsync(project, runUi);
            triggerFilterRefresh(ui, list);
        }
    }

    private void persistRunDataAsync(final @NotNull Project project, final @NotNull RunEditorUI runUi) {
        if (runUi.getTr() == null || runUi.getVf().getTestRun() == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = runUi.getVf().getTestRun().getPath();
                Path jsonFilePath = dirPath.resolve(runUi.getVf().getTestRun().getName() + ".json");
                Services.getInstance(project, FilesUtil.class).write(project, jsonFilePath, runUi.getTr());
            } catch (Exception e) {
                Log.error("Failed to persist test run data: " + e.getMessage());
            }
        });
    }

    private void triggerFilterRefresh(final @NotNull IEditorUI ui, final JBList<TestCaseDto> list) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (list != null) {
                list.repaint();
            }
            if (ui instanceof IToolBar) {
                ((IToolBar) ui).onToolBarFilterSelectionChanged();
            }
        });
    }
}
