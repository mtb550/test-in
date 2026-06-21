package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.markers.TestProjectMarker;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.util.FilesUtil;
import org.testin.util.TreeUtilImpl;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;

public class CreateTestProject extends DumbAwareAction {
    private final ProjectPanel projectPanel;

    public CreateTestProject(final ProjectPanel projectPanel) {
        super("New Test Project", "Create a new test project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
    }

    private String extractProjectNameFromUrl(final String gitUrl) {
        String name = gitUrl;
        if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        if (name.endsWith(".git")) name = name.substring(0, name.length() - 4);
        int lastSlashIndex = name.lastIndexOf('/');
        int lastColonIndex = name.lastIndexOf(':');
        int splitIndex = Math.max(lastSlashIndex, lastColonIndex);
        if (splitIndex != -1 && splitIndex < name.length() - 1) return name.substring(splitIndex + 1);
        return "ImportedTestProject";
    }

    public void execute(final @NotNull Project project) {
        new CreateNodesDialog(project, CreateNodeMenu.TEST_PROJECT, (name, directoryType, codeGenerator) -> {
            if (name == null || name.trim().isEmpty()) return;

            if (directoryType == DirectoryType.IMPORT_TP) {
                String gitUrl = name.trim();
                String projectName = extractProjectNameFromUrl(gitUrl);
                new CloneProject(gitUrl, projectName, projectPanel).execute(project);
                return;
            }

            final String tpName = name.trim();
            final Path tpPath = Services.getInstance(project, Setting.class).getTestinPath().resolve(tpName);

            if (Files.exists(tpPath)) {
                Services.getInstance(project, Notifier.class).error(project, "Creation Failed", "A test project named '" + tpName + "' already exists.");
                return;
            }

            TestProjectDirectoryDto newTp = Services.getInstance(project, DirectoryMapper.class).readTestProjectNode(project, tpPath);

            if (newTp == null) {
                Services.getInstance(project, Notifier.class).error(project, "Creation Failed", "Could not map test project directory in memory.");
                return;
            }

            Services.getInstance(project, TreeUtilImpl.class).executeVfsAction(project, Services.getInstance(project, Setting.class).getTestinPath(), "IO Error", vf -> {

                if (vf.findChild(tpName) != null) {
                    Services.getInstance(project, Notifier.class).error(project, "Creation Failed", "The directory '" + tpName + "' already exists in the IDE's Virtual File System.");
                    return;
                }

                VirtualFile projectDir = vf.createChildDirectory(this, tpName);

                VirtualFile tpFile = projectDir.createChildData(this, DirectoryType.TP.getMarker());

                TestProjectMarker marker = TestProjectMarker.builder()
                        .status(ProjectStatus.ACTIVE)
                        .createdBy(System.getProperty("user.name", ""))
                        .build();

                Services.getInstance(project, FilesUtil.class).write2(project, tpFile, marker);
                newTp.setMarker(marker);


                String tcdName = newTp.getTestCasesDirectory().getPath().getFileName().toString();
                VirtualFile tcdDir = projectDir.createChildDirectory(this, tcdName);
                tcdDir.createChildData(this, DirectoryType.TCD.getMarker());

                String trdName = newTp.getTestRunsDirectory().getPath().getFileName().toString();
                VirtualFile trdDir = projectDir.createChildDirectory(this, trdName);
                trdDir.createChildData(this, DirectoryType.TRD.getMarker());

                projectDir.refresh(false, true);
                projectPanel.getTestProjectSelector().addTestProject(newTp);

                Services.getInstance(project, Notifier.class).info(project, "New Test Project", String.format("Test Project %s has been added", name));

                if (codeGenerator.isSelected()) {
                    GeneratorType.CREATE_TEST_PROJECT.getAction().execute(project, null, newTp.getFqcn());
                }
            });
        }
        ).show();
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        execute(e.getProject());
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