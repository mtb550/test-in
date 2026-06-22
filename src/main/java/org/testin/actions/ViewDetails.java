package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.List;

public class ViewDetails extends DumbAwareAction {
    private final JBList<TestCaseDto> list;
    private final Path path;

    public ViewDetails(final JBList<TestCaseDto> list, final Path path) {
        super("View Details", "", AllIcons.Actions.PreviewDetails);
        this.list = list;
        this.path = path;
        this.registerCustomShortcutSet(KeyboardSet.Enter.getCustomShortcut(), list);

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<TestCaseDto> selected = list.getSelectedValuesList();

        if (selected != null && !selected.isEmpty())
            ViewToolWindowFactory.showPanel(e.getProject(), selected, path, ViewPanel::focusDetailsTab);
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
