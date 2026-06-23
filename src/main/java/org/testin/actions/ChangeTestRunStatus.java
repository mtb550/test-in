package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.runEditor.RunEditorUI;
import org.testin.editorPanel.toolBar.components.StartExecutionBtn;
import org.testin.pojo.*;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.FilesUtil;
import org.testin.util.Mapper;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class ChangeTestRunStatus extends DumbAwareAction {
    private final IEditorUI ui;
    private final JBList<TestCaseDto> list;

    public ChangeTestRunStatus(final IEditorUI ui, final JBList<TestCaseDto> list) {
        super("Change Test Run Status", "Change the status of the current test run", AllIcons.Nodes.Test);
        this.ui = ui;
        this.list = list;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null || !(ui instanceof RunEditorUI runUi)) return;

        TestRunStatus currentStatus = runUi.getVf().getTestRun().getMarker().getStatus();
        TestRunStatus newStatus;

        if (currentStatus == TestRunStatus.CREATED || currentStatus == TestRunStatus.ASSIGNED) {
            newStatus = TestRunStatus.IN_PROGRESS;

        } else if (currentStatus == TestRunStatus.IN_PROGRESS) {
            newStatus = TestRunStatus.COMPLETED;

        } else {
            return;
        }

        applyStatusChange(project, runUi, newStatus);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (!(ui instanceof RunEditorUI runUi)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        TestRunStatus currentStatus = runUi.getVf().getTestRun().getMarker().getStatus();
        boolean enabled = currentStatus == TestRunStatus.CREATED
                || currentStatus == TestRunStatus.ASSIGNED
                || currentStatus == TestRunStatus.IN_PROGRESS;

        e.getPresentation().setEnabled(enabled);

        if (currentStatus == TestRunStatus.IN_PROGRESS) {
            e.getPresentation().setText("Complete Test Run");
            e.getPresentation().setDescription("Mark test run as completed");
            e.getPresentation().setIcon(AllIcons.Actions.Checked);
        } else {
            e.getPresentation().setText("Start Execution");
            e.getPresentation().setDescription("Start execution of test cases");
            e.getPresentation().setIcon(AllIcons.Nodes.Services);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public void applyStatusChange(final @NotNull Project project, final @NotNull RunEditorUI runUi, final @NotNull TestRunStatus newStatus) {
        TestRunMarker marker = runUi.getVf().getTestRun().getMarker();
        TestRunStatus oldStatus = marker.getStatus();

        marker.setStatus(newStatus);
        marker.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        Log.trace("[ChangeTestRunStatus] Test run status changed: "
                + runUi.getVf().getTestRun().getName()
                + " = " + newStatus.getLabel());

        if (newStatus == TestRunStatus.COMPLETED || newStatus == TestRunStatus.CLOSED) {
            resetPendingToUntested(runUi);
        }

        if (newStatus == TestRunStatus.COMPLETED && oldStatus == TestRunStatus.IN_PROGRESS) {
            runUi.stopExecution();
        }

        persistMarker(project, runUi);
        persistResults(project, runUi);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (list != null) list.repaint();
            runUi.getStatusBar().updatePaginationState(
                    runUi.getCurrentPage(),
                    runUi.getTotalPageCount(),
                    runUi.getCurrentTestCases().size(),
                    runUi.getTotalItemsCount()
            );
            updateStartButton(runUi);
        });
    }

    public void onExecutionFinished(final @NotNull Project project, final @NotNull RunEditorUI runUi) {
        runUi.stopExecution();

        TestRunMarker marker = runUi.getVf().getTestRun().getMarker();
        marker.setStatus(TestRunStatus.COMPLETED);
        marker.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        resetPendingToUntested(runUi);

        persistMarker(project, runUi);
        persistResults(project, runUi);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (list != null) list.repaint();
            runUi.getStatusBar().updatePaginationState(
                    runUi.getCurrentPage(),
                    runUi.getTotalPageCount(),
                    runUi.getCurrentTestCases().size(),
                    runUi.getTotalItemsCount()
            );
            updateStartButton(runUi);
        });
    }

    private void resetPendingToUntested(final @NotNull RunEditorUI runUi) {
        Map<UUID, TestRunItems> resultsMap = runUi.getResultsMap();
        for (TestRunItems item : resultsMap.values()) {
            if (item.getStatus() == TestStatus.PENDING) {
                item.setStatus(TestStatus.UNTESTED);
                item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            }
        }
    }

    private void persistMarker(final @NotNull Project project, final @NotNull RunEditorUI runUi) {
        Path markerPath = runUi.getVf().getTestRun().getPath().resolve(DirectoryType.TR.getMarker());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Mapper mapper = Services.getInstance(project, Mapper.class);
                TestRunMarker marker;

                if (Files.exists(markerPath)) {
                    marker = mapper.readValue(markerPath.toFile(), TestRunMarker.class);
                    if (marker != null) {
                        marker.setStatus(runUi.getVf().getTestRun().getMarker().getStatus());
                        marker.setCreatedAt(runUi.getVf().getTestRun().getMarker().getCreatedAt());
                    }
                } else {
                    marker = runUi.getVf().getTestRun().getMarker();
                }

                if (marker != null) {
                    Services.getInstance(project, FilesUtil.class).write(project, markerPath, marker);
                    Log.trace("[ChangeTestRunStatus] Marker persisted -> " + marker.getStatus().getLabel());
                }
            } catch (Exception ex) {
                Log.error("Failed to persist marker: " + ex.getMessage());
            }
        });
    }

    private void persistResults(final @NotNull Project project, final @NotNull RunEditorUI runUi) {
        if (runUi.getTr() == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = runUi.getVf().getTestRun().getPath();
                Services.getInstance(project, ProjectIndexer.class).putTestRun(dirPath, runUi.getTr());
                Log.trace("[ChangeTestRunStatus] Results persisted");
            } catch (Exception e) {
                Log.error("Failed to persist test run results: " + e.getMessage());
            }
        });
    }

    private void updateStartButton(final @NotNull RunEditorUI runUi) {
        StartExecutionBtn startBtn = runUi.getToolBar().getToolbarItem(StartExecutionBtn.class);
        if (startBtn != null) {
            startBtn.updateEnabledState();
        }
    }

}