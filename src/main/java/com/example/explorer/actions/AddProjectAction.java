package com.example.explorer.actions;

import com.example.explorer.ExplorerPanel;
import com.example.pojo.Config;
import com.example.pojo.Directory;
import com.example.util.NodeType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.example.util.Tools.refreshPath;

public class AddProjectAction extends AnAction {
    public final ExplorerPanel panel;

    public AddProjectAction(ExplorerPanel panel) {
        super("New Project", "Add new project", AllIcons.General.Add);
        this.panel = panel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String name = Messages.showInputDialog("Enter project name:", "Add New Project", null);
        if (name == null || name.isBlank()) return;

        Directory newProject = new Directory().setType(NodeType.PROJECT.getCode()).setId(100).setName(name).setActive(1);
        newProject.setFileName(newProject.getType() + "_" + newProject.getId() + "_" + newProject.getName() + "_" + newProject.getActive());
        newProject.setFilePath(Config.getRootFolder().toPath().resolve(newProject.getFileName()));
        newProject.setFile(new File(newProject.getFileName()));

        try {
            Files.createDirectories(newProject.getFilePath());
            System.out.println("Success! project created: " + newProject.getFilePath());
            refreshPath(newProject.getFilePath());

            if (panel.getProjectSelector() != null) {
                panel.getProjectSelector().addAndSelectProject(newProject);
            }

        } catch (IOException ex) {
            System.err.println("Could not create project: " + ex.getMessage());
        }
    }
}