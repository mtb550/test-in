package org.testin.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.editorPanel.toolBar.components.StartExecutionBtn;
import org.testin.enums.TestRunStatus;
import org.testin.enums.TestStatus;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.settings.AppSettingsState;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class UpdateTestRunStatusAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public UpdateTestRunStatusAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Change Test Run Status", "Change the status of the current test run", AllIcons.Nodes.Test);
        this.p = p;
        this.editor = editor;
        this.list = list;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (!(editor instanceof RunEditor runEditor)) return;

        TestRunStatus currentStatus = runEditor.getParent().getMarker().getStatus();
        TestRunStatus newStatus;

        if (currentStatus == TestRunStatus.CREATED || currentStatus == TestRunStatus.ASSIGNED) {
            newStatus = TestRunStatus.IN_PROGRESS;

        } else if (currentStatus == TestRunStatus.IN_PROGRESS) {
            newStatus = TestRunStatus.COMPLETED;

        } else {
            return;
        }

        applyStatusChange(p, runEditor, newStatus);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (!(editor instanceof RunEditor runEditor)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        TestRunStatus currentStatus = runEditor.getParent().getMarker().getStatus();
        boolean enabled = currentStatus == TestRunStatus.CREATED ||
                currentStatus == TestRunStatus.ASSIGNED ||
                currentStatus == TestRunStatus.IN_PROGRESS;

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

    public void applyStatusChange(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull TestRunStatus newStatus) {
        TestRunMarker marker = editor.getParent().getMarker();
        TestRunStatus oldStatus = marker.getStatus();

        marker.setStatus(newStatus);
        marker.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        Logger.trace("Test run status changed: " + editor.getParent().getName() + " = " + newStatus.getLabel());

        if (newStatus == TestRunStatus.COMPLETED || newStatus == TestRunStatus.CLOSED)
            resetPendingToUntested(editor);

        if (newStatus == TestRunStatus.COMPLETED && oldStatus == TestRunStatus.IN_PROGRESS)
            editor.stopExecution();

        persist(p, editor, marker);

        ApplicationManager.getApplication().invokeLater(() -> {
            list.repaint();
            editor.getStatusBar().updatePaginationState(
                    editor.getCurrentPage(),
                    editor.getTotalPageCount(),
                    editor.getTotalItemsCount()
            );

            updateStartButton(editor);
        });
    }

    public void onExecutionFinished(final @NotNull Project p, final @NotNull RunEditor editor) {
        editor.stopExecution();

        TestRunMarker marker = editor.getParent().getMarker();
        marker.setStatus(TestRunStatus.COMPLETED);
        marker.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        resetPendingToUntested(editor);

        persist(p, editor, marker);

        ApplicationManager.getApplication().invokeLater(() -> {
            list.repaint();
            editor.getStatusBar().updatePaginationState(editor.getCurrentPage(), editor.getTotalPageCount(), editor.getTotalItemsCount());
            updateStartButton(editor);
        });
    }

    private void resetPendingToUntested(final @NotNull RunEditor editor) {
        Map<UUID, TestRunItems> resultsMap = editor.getResultsMap();
        for (TestRunItems item : resultsMap.values()) {
            if (item.getStatus() == TestStatus.PENDING) {
                item.setStatus(TestStatus.UNTESTED);
                item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                item.setExecutedBy(Services.getInstance(p, AppSettingsState.class).testerName);
            }
        }
    }

    /**
     * Both writes go through the single-writer RunStatusService: state is
     * snapshotted on the EDT, so later clicks can never tear the persisted JSON.
     */
    private void persist(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull TestRunMarker marker) {
        final RunStatusService statusService = Services.getInstance(p, RunStatusService.class);
        statusService.persistMarker(p, editor.getParent().getPath(), marker.getStatus(), marker.getCreatedAt());
        statusService.persistRun(p, editor);
    }

    private void updateStartButton(final @NotNull RunEditor editor) {
        StartExecutionBtn startBtn = editor.getToolBar().getToolbarItem(StartExecutionBtn.class);
        if (startBtn != null) {
            startBtn.updateEnabledState();
        }
    }

}