package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.DirectoryType;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.util.Tools;
import org.testin.util.services.Services;

public class CreateTestProject extends DumbAwareAction {
    private final @NotNull ProjectPanel projectPanel;

    public CreateTestProject(final @NotNull ProjectPanel projectPanel) {
        super("New Test Project", "Create or Clone test project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        new CreateNodesDialog(project, CreateNodeMenu.TEST_PROJECT, (name, type, cg) -> {
            if (name.trim().isEmpty()) return;

            if (type == DirectoryType.IMPORT_TP) {
                String gitUrl = name.trim();
                String projectName = Services.getInstance(project, Tools.class).extractProjectNameFromUrl(gitUrl);
                new CreateTestProjectClone(gitUrl, projectName, projectPanel).actionPerformed(e);
                return;
            }

            if (type == DirectoryType.TP) {
                final String tpName = name.trim();
                new CreateTestProjectNew(projectPanel, tpName, cg).actionPerformed(e);
                //return;
            }

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (e.getProject() == null || Services.getInstance(e.getProject(), Setting.class).getTestinPath().toString().isEmpty())
            e.getPresentation().setEnabled(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}