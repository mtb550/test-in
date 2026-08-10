package org.testin.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testRun.updateDialog.RunItemUpdateMenuDialog;
import org.testin.util.Shortcuts;

import java.nio.file.Path;

public class UpdateRunItemAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public UpdateRunItemAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Update Test Run Item", "Update test run item attributes", AllIcons.Actions.Edit);
        this.p = p;
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.UpdateItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final TestCaseDto selected = list.getSelectedValue();
        if (selected == null) return;

        if (!(editor instanceof RunEditor runEditor)) return;

        final TestRunItems runItem = runEditor.getResultsMap().get(selected.getId());
        if (runItem == null) return;

        Logger.trace("update test run item for: " + selected.getDescription());

        new RunItemUpdateMenuDialog(p, runItem, updatedItem -> {
            Logger.trace("run item updated, actual result: " + updatedItem.getActualResult());

            if (runEditor.getParent() != null) {
                Path dirPath = runEditor.getParent().getPath();
                Services.getInstance(p, ProjectIndexer.class).putTestRun(dirPath, runEditor.getTr());
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
