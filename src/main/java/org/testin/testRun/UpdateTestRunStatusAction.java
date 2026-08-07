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
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.settings.AppSettingsState;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class UpdateTestRunStatusAction extends DumbAwareAction {
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public UpdateTestRunStatusAction(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Change Test Run Status", "Change the status of the current test run", AllIcons.Nodes.Test);
        this.editor = editor;
        this.list = list;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project p = e.getProject();
        if (p == null || !(editor instanceof RunEditor runEditor)) return;

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

        persistMarker(p, editor);
        persistResults(p, editor);

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

        persistMarker(p, editor);
        persistResults(p, editor);

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
                item.setExecutedBy(AppSettingsState.getInstance().testerName);
            }
        }
    }

    private void persistMarker(final @NotNull Project p, final @NotNull RunEditor editor) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                final Path runPath = editor.getParent().getPath();
                final TestRunDirectoryDto trd = indexer.getTestRunDirByPath(runPath);

                if (trd == null) return;

                TestRunMarker marker = trd.getMarker();

                marker.setStatus(editor.getParent().getMarker().getStatus());
                marker.setCreatedAt(editor.getParent().getMarker().getCreatedAt());

                indexer.updateRunMarker(p, runPath, marker);
                Logger.trace("Marker persisted -> " + marker.getStatus().getLabel());
            } catch (final Exception ex) {
                Logger.error("Failed to persist marker: " + ex.getMessage());
            }
        });
    }

    private void persistResults(final @NotNull Project p, final @NotNull RunEditor editor) {
        if (editor.getTr() == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = editor.getParent().getPath();
                Services.getInstance(p, ProjectIndexer.class).putTestRun(dirPath, editor.getTr());
                Logger.trace("Results persisted");
            } catch (final Exception ex) {
                Logger.error("Failed to persist test run results: " + ex.getMessage());
            }
        });
    }

    private void updateStartButton(final @NotNull RunEditor editor) {
        StartExecutionBtn startBtn = editor.getToolBar().getToolbarItem(StartExecutionBtn.class);
        if (startBtn != null) {
            startBtn.updateEnabledState();
        }
    }

}