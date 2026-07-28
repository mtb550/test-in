package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.Dialogs.testRun.ActualResultDialog;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Logger;

public class SetActualResult extends DumbAwareAction {

    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public SetActualResult(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Actual Result", "Set actual result for test case", AllIcons.Actions.Copy);
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.SetActualResult.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        final TestCaseDto selected = list.getSelectedValue();
        if (selected == null) return;

        if (!(editor instanceof RunEditor runEditor)) return;

        final TestRunItems runItem = runEditor.getResultsMap().get(selected.getId());
        if (runItem == null) return;

        Logger.trace("set actual result for: " + selected.getDescription());

        new ActualResultDialog(project, runItem).show();
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
