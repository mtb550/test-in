package org.testin.testrun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.components.StartExecutionBtn;
import org.testin.logger.Logger;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class UpdateTestRunStatusAction extends AbstractProjectAction {
    private final @NotNull TestinEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public UpdateTestRunStatusAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Change Test Run Status", "Change the status of the current test run", AllIcons.Nodes.Test);
        this.editor = editor;
        this.list = list;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (!(editor instanceof RunEditor runEditor)) return;

        // The enum owns the lifecycle: ask it where this run goes next.
        final TestRunStatus newStatus = runEditor.getParent().getMarker().getStatus().nextStatus();
        if (newStatus == null) return;

        applyStatusChange(runEditor, newStatus);
    }

    public void applyStatusChange(final @NotNull RunEditor editor, final @NotNull TestRunStatus newStatus) {
        TestRunMarker marker = editor.getParent().getMarker();
        TestRunStatus oldStatus = marker.getStatus();

        marker.setStatus(newStatus);
        marker.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        Logger.trace("Test run status changed: " + editor.getParent().getName() + " = " + newStatus.getLabel());

        if (newStatus.isTerminal())
            resetPendingToUntested(editor);

        if (newStatus == TestRunStatus.COMPLETED && oldStatus == TestRunStatus.IN_PROGRESS)
            editor.stopExecution();

        persist(editor, marker);

        ApplicationManager.getApplication().invokeLater(() -> {
            list.repaint();
            editor.getStatusBar().updatePaginationState(
                    editor.getCurrentPage(),
                    editor.getTotalPageCount(),
                    editor.getTotalItemsCount()
            );

            updateStartButton(editor);
        });

        // The status names itself. Start Run routes through here rather than
        // notifying for itself, so pressing it says "In Progress" once (#62).
        Services.getInstance(p, Notifier.class).softShow(p, newStatus.getLabel());
    }

    public void onExecutionFinished(final @NotNull RunEditor editor) {
        editor.stopExecution();

        TestRunMarker marker = editor.getParent().getMarker();
        marker.setStatus(TestRunStatus.COMPLETED);
        marker.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        resetPendingToUntested(editor);

        persist(editor, marker);

        ApplicationManager.getApplication().invokeLater(() -> {
            list.repaint();
            editor.getStatusBar().updatePaginationState(editor.getCurrentPage(), editor.getTotalPageCount(), editor.getTotalItemsCount());
            updateStartButton(editor);
        });

        Services.getInstance(p, Notifier.class).softShow(p, TestRunStatus.COMPLETED.getLabel());
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
    private void persist(final @NotNull RunEditor editor, final @NotNull TestRunMarker marker) {
        final RunStatusService statusService = Services.getInstance(p, RunStatusService.class);
        statusService.persistMarker(p, editor.getParent().getPath(), marker.getStatus(), marker.getCreatedAt());
        statusService.persistRun(p, editor);
    }

    private void updateStartButton(final @NotNull RunEditor editor) {
        editor.getToolBar().getToolbarItem(StartExecutionBtn.class).updateEnabledState();
    }

}
