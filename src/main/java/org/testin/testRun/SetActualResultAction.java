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
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.testRun.createDialog.ActualResultDialog;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Logger;

public class SetActualResultAction extends DumbAwareAction {

    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public SetActualResultAction(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Actual Result", "Set actual result for test case", AllIcons.Actions.Copy);
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.SetActualResult.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project p = e.getProject();
        if (p == null) return;

        final TestCaseDto selected = list.getSelectedValue();
        if (selected == null) return;

        if (!(editor instanceof RunEditor runEditor)) return;

        final TestRunItems runItem = runEditor.getResultsMap().get(selected.getId());
        if (runItem == null) return;

        Logger.trace("set actual result for: " + selected.getDescription());

        new ActualResultDialog(p, runItem).show();
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
