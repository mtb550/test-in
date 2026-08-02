package org.testin.testProject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateNodeMenu;
import org.testin.enums.DirectoryType;
import org.testin.nodeCreator.dialogs.CreateNodesDialog;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.util.Tools;
import org.testin.util.services.Services;

public class CreateTestProjectAction extends DumbAwareAction {
    private final @NotNull ProjectPanel projectPanel;

    public CreateTestProjectAction(final @NotNull ProjectPanel projectPanel) {
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
                new CreateTestProjectCloneAction(gitUrl, projectName, projectPanel).actionPerformed(e);
                return;
            }

            if (type == DirectoryType.TP) {
                final String tpName = name.trim();
                new CreateTestProjectNewAction(projectPanel, tpName, cg).actionPerformed(e);
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