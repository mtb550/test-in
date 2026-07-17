package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.pojo.markers.TestProjectMarker;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;

public class CreateTestProject extends DumbAwareAction {
    private final @NotNull ProjectPanel projectPanel;

    public CreateTestProject(final @NotNull ProjectPanel projectPanel) {
        super("New Test Project", "Create a new test project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        new CreateNodesDialog(project, CreateNodeMenu.TEST_PROJECT, (name, directoryType, codeGenerator) -> {
            if (name == null || name.trim().isEmpty()) return;

            if (directoryType == DirectoryType.IMPORT_TP) {
                String gitUrl = name.trim();
                String projectName = Services.getInstance(project, Tools.class).extractProjectNameFromUrl(gitUrl);
                new CloneProject(gitUrl, projectName, projectPanel).actionPerformed(e);
                return;
            }

            final String tpName = name.trim();
            final Path tpPath = Services.getInstance(project, Setting.class).getTestinPath().resolve(tpName);

            if (Files.exists(tpPath)) {
                Services.getInstance(project, Notifier.class).error(project, "Creation Failed", "A test project named '" + tpName + "' already exists.");
                return;
            }

            final String fileName = tpPath.getFileName().toString();
            final TestProjectDirectoryDto newTp = TestProjectDirectoryDto.builder()
                    .name(fileName)
                    .path(tpPath)
                    .pathName(fileName)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(null, fileName))
                    .build();

            final TestCasesMainDirectoryDto tcd = TestCasesMainDirectoryDto.builder()
                    .path(tpPath.resolve(DirectoryType.TCD.getDisplayedName()))
                    .name(DirectoryType.TCD.getDisplayedName())
                    .parent(newTp)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(newTp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                    .build();

            final TestRunsMainDirectoryDto trd = TestRunsMainDirectoryDto.builder()
                    .path(tpPath.resolve(DirectoryType.TRD.getDisplayedName()))
                    .name(DirectoryType.TRD.getDisplayedName())
                    .parent(newTp)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(newTp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                    .build();

            newTp.setTestCasesDirectory(tcd);
            newTp.setTestRunsDirectory(trd);

            Services.getInstance(project, TreeUtilImpl.class).executeVfsAction(project, Services.getInstance(project, Setting.class).getTestinPath(), "IO Error", vf -> {

                if (vf.findChild(tpName) != null) {
                    Services.getInstance(project, Notifier.class).error(project, "Creation Failed", "The directory '" + tpName + "' already exists in the IDE's Virtual File System.");
                    return;
                }

                VirtualFile projectDir = vf.createChildDirectory(this, tpName);

                projectDir.createChildData(this, DirectoryType.TP.getMarker());

                TestProjectMarker marker = TestProjectMarker.builder()
                        .status(ProjectStatus.ACTIVE)
                        .createdBy(System.getProperty("user.name", ""))
                        .createdAt(ZonedDateTime.now())
                        .build();

                newTp.setMarker(marker);

                Services.getInstance(project, ProjectIndexer.class).persistTestProjectMarker(project, newTp);

                String tcdName = newTp.getTestCasesDirectory().getPath().getFileName().toString();
                VirtualFile tcdDir = projectDir.createChildDirectory(this, tcdName);
                tcdDir.createChildData(this, DirectoryType.TCD.getMarker());

                String trdName = newTp.getTestRunsDirectory().getPath().getFileName().toString();
                VirtualFile trdDir = projectDir.createChildDirectory(this, trdName);
                trdDir.createChildData(this, DirectoryType.TRD.getMarker());

                projectDir.refresh(false, true);
                projectPanel.getTestProjectSelector().addTestProject(newTp);

                Services.getInstance(project, ProjectIndexer.class).addTestProject(newTp);
                Services.getInstance(project, ProjectPanel.class).getProjectTree().updateNodes();
                Services.getInstance(project, Notifier.class).info(project, "New Test Project", String.format("Test Project %s has been added", name));

                if (codeGenerator.isSelected()) {
                    GeneratorType.CREATE_TEST_PROJECT.getAction().execute(project, null, newTp.getPath2());
                }
            });
        }
        ).show();
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