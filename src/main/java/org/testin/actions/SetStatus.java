package org.testin.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.testRunEditor.RunEditorUI;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.FilesUtil;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// todo, to be refactored.
public class SetStatus {

    private final IEditorUI ui;
    private final JBList<TestCaseDto> list;

    public SetStatus(final IEditorUI ui, final JBList<TestCaseDto> list) {
        this.ui = ui;
        this.list = list;
    }

    public void executeManual(@NotNull Project project, @NotNull TestCaseDto tc, @NotNull TestStatus status) {
        if (!(ui instanceof RunEditorUI runUi)) return;

        TestRunItems item = runUi.getResultsMap().get(tc.getId());
        if (item == null) return;

        int tcIndex = runUi.getCurrentTestCases().indexOf(tc);
        if (tcIndex != -1 && tcIndex == runUi.getCurrentlyExecutingIndex()) {
            runUi.stopExecution();
        }

        item.setStatus(status);
        item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        Log.trace("[SetStatus]: Status updated -> " + tc.getDescription() + " = " + status);

        persistRunDataAsync(runUi);

        triggerFilterRefresh();
    }

    public void executeNext(@NotNull Project project, @NotNull TestStatus status) {
        if (!(ui instanceof RunEditorUI runUi)) return;

        int executingIndex = runUi.getCurrentlyExecutingIndex();
        if (executingIndex == -1) return;

        TestCaseDto currentTc = runUi.getCurrentTestCases().get(executingIndex);
        TestRunItems item = runUi.getResultsMap().get(currentTc.getId());

        if (item != null) {
            item.setStatus(status);
            item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        }

        Log.trace("[SetStatus]: Execution status updated -> " + currentTc.getDescription() + " = " + status);

        persistRunDataAsync(runUi);

        triggerFilterRefresh();

        ApplicationManager.getApplication().invokeLater(() -> {
            UUID currentId = currentTc.getId();
            boolean stillInList = runUi.getCurrentTestCases().stream()
                    .anyMatch(t -> t.getId().equals(currentId));
            int nextIndex = stillInList ? executingIndex + 1 : executingIndex;
            runUi.startTimerForIndex(nextIndex);
        });
    }

    private void persistRunDataAsync(final @NotNull RunEditorUI runUi) {
        if (runUi.getTr() == null || runUi.getVf().getTestRun() == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Path dirPath = runUi.getVf().getTestRun().getPath();
            Path jsonFilePath = dirPath.resolve(runUi.getVf().getTestRun().getName() + ".json");

            Services.getInstance(runUi.getProject(), FilesUtil.class).write(runUi.getProject(), jsonFilePath, runUi.getTr());
        });
    }

    private void triggerFilterRefresh() {
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