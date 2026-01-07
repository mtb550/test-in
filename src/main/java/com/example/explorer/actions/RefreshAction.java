package com.example.explorer.actions;

import com.example.explorer.ComboBoxProjectSelector;
import com.example.explorer.ProjectPanel;
import com.example.pojo.Directory;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class RefreshAction extends AnAction {
    private final ProjectPanel projectPanel;

    public RefreshAction(ProjectPanel projectPanel) {
        super("Refresh", "Reload tree", AllIcons.Actions.Refresh);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        //refreshPath(Config.getRootFolder().toPath());
        Directory selectedProject = ComboBoxProjectSelector.getSelectedProject();

        if (selectedProject != null) {
            projectPanel.filterByProject(selectedProject);
            System.out.println("refresh project: " + selectedProject.getName());
        } else {
            projectPanel.loadAllProjects();
            System.out.println("refresh all projects");
        }
    }
}
