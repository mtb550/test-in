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
    private final @NotNull Project p;
    private final @NotNull ProjectPanel projectPanel;

    public CreateTestProjectAction(final @NotNull Project p, final @NotNull ProjectPanel projectPanel) {
        super("New Test Project", "Create or Clone test project", AllIcons.General.Add);
        this.p = p;
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        new CreateNodesDialog(p, CreateNodeMenu.TEST_PROJECT, (name, type, cg) -> {
            if (name.trim().isEmpty()) return;

            if (type == DirectoryType.IMPORT_TP) {
                String projectName = Services.getInstance(p, Tools.class).extractProjectNameFromUrl(name);
                new CreateTestProjectCloneAction(p, name.trim(), projectName, projectPanel).actionPerformed(e);
                return;
            }

            if (type == DirectoryType.TP) {
                new CreateTestProjectNewAction(p, projectPanel, name.trim(), cg).actionPerformed(e);
            }

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (Services.getInstance(p, Setting.class).getTestinPath().toString().isEmpty())
            e.getPresentation().setEnabled(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}