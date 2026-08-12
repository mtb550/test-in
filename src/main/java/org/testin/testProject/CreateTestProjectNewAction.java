package org.testin.testProject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;
import org.testin.settings.Setting;

import java.nio.file.Path;

public class CreateTestProjectNewAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull ProjectPanel pp;
    private final @NotNull String tpName;

    public CreateTestProjectNewAction(final @NotNull Project p, final @NotNull ProjectPanel pp, final @NotNull String name) {
        super("New Test Project", "Create a new test project", AllIcons.General.Add);
        this.p = p;
        this.pp = pp;
        this.tpName = name;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    /**
     * Direct entry point for dialog callbacks — no AnActionEvent required.
     */
    public void execute() {

        final Path tpPath = Services.getInstance(p, Setting.class).getTestinPath().resolve(tpName);

        if (Services.getInstance(p, ProjectIndexer.class).projectExists(tpPath)) {
            Services.getInstance(p, Notifier.class).error(p, "Creation Failed", "A test project named '" + tpName + "' already exists.");
            return;
        }

        final TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).setTestProjectNode(p, tpPath);

        Services.getInstance(p, ProjectIndexer.class).addTestProject(tp);
        pp.getTestProjectSelector().addTestProject(tp);
        pp.getProjectTree().updateNodes();
        Services.getInstance(p, Notifier.class).info(p, "New Test Project", String.format("Test Project %s has been added", tpName));

        GeneratorType.CREATE_TEST_PROJECT.getAction().execute(p, tp);
    }


    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Both branches, otherwise the action stays disabled for the whole session
        // once seen without a configured Testin root.
        e.getPresentation().setEnabled(!Services.getInstance(p, Setting.class).getTestinPath().toString().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}