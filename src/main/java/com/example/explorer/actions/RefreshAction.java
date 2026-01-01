package com.example.explorer.actions;

import com.example.explorer.ComboBoxProjectSelector;
import com.example.explorer.ExplorerPanel;
import com.example.pojo.Config;
import com.example.pojo.Directory;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import static com.example.util.Tools.refreshPath;

public class RefreshAction extends AnAction {
    private final ExplorerPanel panel;

    public RefreshAction(ExplorerPanel panel) {
        super("Refresh", "Reload tree", AllIcons.Actions.Refresh);
        this.panel = panel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        refreshPath(Config.getRootFolder().toPath());
        Directory selectedProject = ComboBoxProjectSelector.getSelectedProject();

        if (selectedProject != null) {
            panel.filterByProject(selectedProject);
            System.out.println("refresh project: " + selectedProject.getName());
        } else {
            panel.loadAllProjects();
            System.out.println("refresh all projects");
        }
    }
}
