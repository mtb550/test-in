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
import org.testin.ui.testRun.ActualResultUI;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Log;

public class SetActualResult extends DumbAwareAction {

    private final IEditorUI ui;
    private final JBList<TestCaseDto> list;

    public SetActualResult(final IEditorUI ui, final JBList<TestCaseDto> list) {
        super("Actual Result", "Set actual result for test case", AllIcons.Actions.Copy);
        this.ui = ui;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.SetActualResult.getCustomShortcut(), list);
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

        Log.trace("set actual result for: " + selected.getDescription());

        new ActualResultUI(project, runItem).show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(ui instanceof RunEditorUI);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
