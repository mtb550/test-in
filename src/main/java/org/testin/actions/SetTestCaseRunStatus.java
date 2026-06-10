package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.testRunEditor.RunEditorUI;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.FilesUtil;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// todo, to be refactored
public class SetTestCaseRunStatus extends DumbAwareAction {

    private final IEditorUI ui;
    private final JBList<TestCaseDto> list;

    public SetTestCaseRunStatus(final IEditorUI ui, final JBList<TestCaseDto> list) {
        super("Set Status", "Set test case status", AllIcons.General.Filter);
        this.ui = ui;
        this.list = list;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(ui instanceof RunEditorUI && list != null && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Sub-menu container — toggle actions handle the actual status change.
    }

    public List<AnAction> getStatusActions() {
        List<AnAction> actions = new ArrayList<>();

        // PASSED with CTRL+P shortcut
        DumbAwareToggleAction passedAction = new DumbAwareToggleAction("Passed") {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return false;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                if (!state) return;
                applyStatus(TestStatus.PASSED);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(ui instanceof RunEditorUI);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };

        passedAction.registerCustomShortcutSet(KeyboardSet.SetStatusPassed.getCustomShortcut(), list);
        actions.add(passedAction);

        // FAILED with CTRL+F shortcut
        DumbAwareToggleAction failedAction = new DumbAwareToggleAction("Failed") {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return false;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                if (!state) return;
                applyStatus(TestStatus.FAILED);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(ui instanceof RunEditorUI);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
        failedAction.registerCustomShortcutSet(KeyboardSet.SetStatusFailed.getCustomShortcut(), list);
        actions.add(failedAction);

        // Other statuses without shortcuts
        TestStatus[] others = {TestStatus.BLOCKED, TestStatus.PENDING, TestStatus.UNTESTED};
        for (TestStatus s : others) {
            actions.add(new DumbAwareToggleAction(s.name()) {
                @Override
                public boolean isSelected(@NotNull AnActionEvent e) {
                    return false;
                }

                @Override
                public void setSelected(@NotNull AnActionEvent e, boolean state) {
                    if (!state) return;
                    applyStatus(s);
                }

                @Override
                public void update(@NotNull AnActionEvent e) {
                    e.getPresentation().setEnabled(ui instanceof RunEditorUI);
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.BGT;
                }
            });
        }

        return actions;
    }

    private void applyStatus(TestStatus status) {
        System.out.println("apply status: " + status);
        if (!(ui instanceof RunEditorUI runUi)) return;
        if (list == null) return;

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        for (TestCaseDto tc : selectedItems) {
            TestRunItems item = runUi.getResultsMap().get(tc.getId());
            if (item != null) {
                item.setStatus(status);
                item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

                // Stop execution if this is the currently executing test
                int tcIndex = runUi.getCurrentTestCases().indexOf(tc);
                if (tcIndex != -1 && tcIndex == runUi.getCurrentlyExecutingIndex()) {
                    runUi.stopExecution();
                }
            }
        }

        Log.trace("[SetTestCaseStatus]: Status set to " + status + " for " + selectedItems.size() + " test cases");

        // Persist
        persistRunDataAsync(runUi);

        // Re-apply filter
        ApplicationManager.getApplication().invokeLater(() -> {
            list.repaint();
            ((IToolBar) ui).onToolBarFilterSelectionChanged();
        });
    }

    private void persistRunDataAsync(final @NotNull RunEditorUI runUi) {
        if (runUi.getTr() == null || runUi.getVf().getTestRun() == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = runUi.getVf().getTestRun().getPath();
                Path jsonFilePath = dirPath.resolve(runUi.getTr().getRunName());

                Services.getInstance(runUi.getProject(), FilesUtil.class).write(runUi.getProject(), jsonFilePath, runUi.getTr());

            } catch (Exception e) {
                Log.error("Failed to persist test run data: " + e.getMessage());
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
