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
import org.testin.logger.Logger;
import org.testin.model.TestRunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.RunStatusService;
import org.testin.services.Services;


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
        runEditor.getParent().getMarker().getStatus().nextStatus()
                .ifPresent(newStatus -> applyStatusChange(runEditor, newStatus));
    }

    public void applyStatusChange(final @NotNull RunEditor editor, final @NotNull TestRunStatus newStatus) {
        TestRunMarker marker = editor.getParent().getMarker();
        TestRunStatus oldStatus = marker.getStatus();

        marker.setStatus(newStatus);

        Logger.trace("Test run status changed: " + editor.getParent().getName() + " = " + newStatus.getLabel());

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

            editor.refreshExecutionButtons();
        });

        // The status names itself. Start Run routes through here rather than
        // notifying for itself, so pressing it says "In Progress" once (#62).
        Services.getInstance(p, Notifier.class).softShow(p, newStatus.getLabel());
    }

    public void onExecutionFinished(final @NotNull RunEditor editor) {
        editor.stopExecution();

        TestRunMarker marker = editor.getParent().getMarker();
        marker.setStatus(TestRunStatus.COMPLETED);

        persist(editor, marker);

        ApplicationManager.getApplication().invokeLater(() -> {
            list.repaint();
            editor.getStatusBar().updatePaginationState(editor.getCurrentPage(), editor.getTotalPageCount(), editor.getTotalItemsCount());
            editor.refreshExecutionButtons();
        });

        Services.getInstance(p, Notifier.class).softShow(p, TestRunStatus.COMPLETED.getLabel());
    }


    /**
     * Both writes go through the single-writer RunStatusService: state is
     * snapshotted on the EDT, so later clicks can never tear the persisted JSON.
     */
    private void persist(final @NotNull RunEditor editor, final @NotNull TestRunMarker marker) {
        final @NotNull RunStatusService statusService = Services.getInstance(p, RunStatusService.class);
        statusService.persistMarker(p, editor.getParent().getPath(), marker.getStatus());
        statusService.persistRun(p, editor);
    }

}
