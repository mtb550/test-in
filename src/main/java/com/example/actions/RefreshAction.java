package com.example.actions;

import com.example.projectPanel.ComboBoxProjectSelector;
import com.example.projectPanel.ProjectPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class RefreshAction extends AnAction {
    private final ProjectPanel projectPanel;

    public RefreshAction(final ProjectPanel projectPanel) {
        super("Refresh", "Reload tree", AllIcons.Actions.Refresh);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("RefreshAction.actionPerformed()");

        projectPanel.getProjectSelector().loadProjects();
        projectPanel.getTestCaseTree().removeAll();

        if (projectPanel.getProjectSelector().selected() != null) {

            //projectPanel.setupTestCaseTree();
            projectPanel.filterByProject(ComboBoxProjectSelector.getSelectedProject());
            System.out.println("refresh project: " + projectPanel.getProjectSelector().selected().getName());


        } else {
            System.out.println("no projects");
        }
    }
}
