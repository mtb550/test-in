package org.testin.testProject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.CodeGeneratorDialog;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;

public class CreateTestProjectNewAction extends DumbAwareAction {
    private final @NotNull ProjectPanel projectPanel;
    private final @NotNull String tpName;
    private final @NotNull CodeGeneratorDialog cg;

    public CreateTestProjectNewAction(final @NotNull ProjectPanel projectPanel, final @NotNull String name, final @NotNull CodeGeneratorDialog cg) {
        super("New Test Project", "Create a new test project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tpName = name;
        this.cg = cg;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        final Path tpPath = Services.getInstance(project, Setting.class).getTestinPath().resolve(tpName);

        if (Files.exists(tpPath)) {
            Services.getInstance(project, Notifier.class).error(project, "Creation Failed", "A test project named '" + tpName + "' already exists.");
            return;
        }

        final TestProjectDirectoryDto tp = Services.getInstance(project, DirectoryMapper.class).setTestProjectNode(project, tpPath);

        // The indexer owns all dir/file creation: it creates the project dir, the
        // Test Cases/Test Runs main dirs and writes their marker JSON.
        Services.getInstance(project, ProjectIndexer.class).addTestProject(tp);
        projectPanel.getTestProjectSelector().addTestProject(tp);
        projectPanel.getProjectTree().updateNodes();
        Services.getInstance(project, Notifier.class).info(project, "New Test Project", String.format("Test Project %s has been added", tpName));

        if (cg.isSelected())
            GeneratorType.CREATE_TEST_PROJECT.getAction().execute(project, tp);
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