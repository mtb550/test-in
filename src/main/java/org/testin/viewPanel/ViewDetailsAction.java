package org.testin.viewPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;

public class ViewDetailsAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull ArrayList<String> path;

    public ViewDetailsAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list, final @NotNull ArrayList<String> path) {
        super("View Details", "", AllIcons.Actions.PreviewDetails);
        this.p = p;
        this.list = list;
        this.path = path;
        this.registerCustomShortcutSet(Shortcuts.Enter.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        List<TestCaseDto> selected = list.getSelectedValuesList();

        if (selected != null && !selected.isEmpty())
            ViewToolWindowFactory.showPanel(p, selected, path, ViewPanel::focusDetailsTab);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
