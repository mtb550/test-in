package org.testin.testproject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.codegen.GenType;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.DirectoryMapper;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;

import java.nio.file.Path;

public class CreateTestProjectNewAction extends AbstractProjectAction {
    private final @NotNull ExplorerPanel pp;
    private final @NotNull String tpName;

    public CreateTestProjectNewAction(final @NotNull Project p, final @NotNull ExplorerPanel pp, final @NotNull String name) {
        super(p, "New Test Project", "Create a new test project", AllIcons.General.Add);
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

        final Path tpPath = Services.getInstance(p, TestinRoot.class).getPath().resolve(tpName);

        // The same situation rename already reports this way: the tester typed a
        // name that is taken, which is feedback on what they typed rather than a
        // failure to record in the log (#62).
        if (Services.getInstance(p, ProjectIndexer.class).projectExists(tpPath)) {
            Services.getInstance(p, Notifier.class).softShow(p, tpName + " Already Exists");
            return;
        }

        final TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).setTestProjectNode(p, tpPath);

        Services.getInstance(p, ProjectIndexer.class).addTestProject(tp);
        pp.getTestProjectSelector().addTestProject(tp);
        pp.getProjectTree().updateNodes();
        Services.getInstance(p, Notifier.class).softShow(p, "Project created");

        GenType.CREATE_TEST_PROJECT.getAction().execute(p, tp);
    }


    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Both branches, otherwise the action stays disabled for the whole session
        // once seen without a configured Testin root.
        e.getPresentation().setEnabled(!Services.getInstance(p, TestinRoot.class).getPath().toString().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
