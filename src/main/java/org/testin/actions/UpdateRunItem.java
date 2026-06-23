package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.runEditor.RunEditorUI;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.ui.testRun.update.RunItemUpdateMenu;
import org.testin.util.KeyboardSet;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.nio.file.Path;

public class UpdateRunItem extends DumbAwareAction {

    private final IEditorUI ui;
    private final JBList<TestCaseDto> list;

    public UpdateRunItem(final IEditorUI ui, final JBList<TestCaseDto> list) {
        super("Update Test Run Item", "Update test run item attributes", AllIcons.Actions.Edit);
        this.ui = ui;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestRunItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (list == null || project == null) return;

        final TestCaseDto selected = list.getSelectedValue();
        if (selected == null) return;

        if (!(ui instanceof RunEditorUI runUi)) return;

        final TestRunItems runItem = runUi.getResultsMap().get(selected.getId());
        if (runItem == null) return;

        Log.trace("update test run item for: " + selected.getDescription());

        new RunItemUpdateMenu(project, runItem, updatedItem -> {
            Log.trace("run item updated, actual result: " + updatedItem.getActualResult());

            if (runUi.getTr() != null && runUi.getVf().getTestRun() != null) {
                Path dirPath = runUi.getVf().getTestRun().getPath();
                Services.getInstance(project, ProjectIndexer.class).putTestRun(dirPath, runUi.getTr());
            }

            list.repaint();
        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
