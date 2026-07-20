package org.testin.actions.crud;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.util.TreeUtilImpl;
import org.testin.util.autoGenerator.CodeGenerator;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;

public class CreateTestProjectNew extends DumbAwareAction {
    private final @NotNull ProjectPanel projectPanel;
    private final @NotNull String tpName;
    private final @NotNull CodeGenerator cg;

    public CreateTestProjectNew(final @NotNull ProjectPanel projectPanel, final @NotNull String name, final @NotNull CodeGenerator cg) {
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

        Services.getInstance(project, TreeUtilImpl.class).executeVfsAction(project, Services.getInstance(project, Setting.class).getTestinPath(), "IO Error", vf -> {

            if (vf.findChild(tpName) != null) {
                Services.getInstance(project, Notifier.class).error(project, "Creation Failed", "The directory '" + tpName + "' already exists in the IDE's Virtual File System.");
                return;
            }

            VirtualFile projectDir = vf.createChildDirectory(this, tpName);

            projectDir.createChildData(this, DirectoryType.TP.getMarker());

            String tcdName = tp.getTestCasesDirectory().getPath().getFileName().toString();
            VirtualFile tcdDir = projectDir.createChildDirectory(this, tcdName);
            tcdDir.createChildData(this, DirectoryType.TCD.getMarker());

            String trdName = tp.getTestRunsDirectory().getPath().getFileName().toString();
            VirtualFile trdDir = projectDir.createChildDirectory(this, trdName);
            trdDir.createChildData(this, DirectoryType.TRD.getMarker());

            projectDir.refresh(false, true);
            projectPanel.getTestProjectSelector().addTestProject(tp);

            Services.getInstance(project, ProjectIndexer.class).addTestProject(tp);

            projectPanel.getProjectTree().updateNodes();
            Services.getInstance(project, Notifier.class).info(project, "New Test Project", String.format("Test Project %s has been added", tpName));

            if (cg.isSelected())
                GeneratorType.CREATE_TEST_PROJECT.getAction().execute(project, tp);

        });
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