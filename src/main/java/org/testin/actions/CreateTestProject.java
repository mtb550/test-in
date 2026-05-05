package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.TestCasesDirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunsDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.ui.testCase.GenerateOrUpdateCodeCheckBox;
import org.testin.util.TreeUtilImpl;
import org.testin.util.notifications.Notifier;

import java.nio.file.Path;

public class CreateTestProject extends DumbAwareAction {
    private final ProjectPanel projectPanel;

    public CreateTestProject(ProjectPanel projectPanel) {
        super("New Test Project", "Create a new test project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
    }

    public void execute() {
        GenerateOrUpdateCodeCheckBox checkbox = new GenerateOrUpdateCodeCheckBox(null);

        new CreateNodesDialog(
                CreateNodeMenu.TEST_PROJECT,
                checkbox,
                (name, selectedType) -> {

                    if (name == null || name.trim().isEmpty()) return;

                    String processedName = name.replace("_", " ");

                    TestProjectDirectoryDto newTestProjectDirectory = new TestProjectDirectoryDto()
                            .setName(processedName);

                    String folderName = newTestProjectDirectory.getName();
                    Path projectPath = Config.getTestinPath().resolve(folderName);

                    newTestProjectDirectory.setPathName(folderName)
                            .setPath(projectPath);

                    newTestProjectDirectory
                            .setTestCasesDirectory(
                                    new TestCasesDirectoryDto()
                                            .setPath(newTestProjectDirectory.getPath().resolve(DirectoryType.TCD.getPathName()))
                                            .setName(DirectoryType.TCD.getDisplayedName())
                            )
                            .setTestRunsDirectory(
                                    new TestRunsDirectoryDto()
                                            .setPath(newTestProjectDirectory.getPath().resolve(DirectoryType.TRD.getPathName()))
                                            .setName(DirectoryType.TRD.getDisplayedName())
                            );

                    TreeUtilImpl.executeVfsAction(Config.getTestinPath(), "IO Error", vf -> {

                        VirtualFile projectDir = vf.createChildDirectory(this, folderName);
                        projectDir.createChildData(this, DirectoryType.TP.getMarker());

                        String tcdName = newTestProjectDirectory.getTestCasesDirectory().getPath().getFileName().toString();
                        VirtualFile tcdDir = projectDir.createChildDirectory(this, tcdName);
                        tcdDir.createChildData(this, DirectoryType.TCD.getMarker());

                        String trdName = newTestProjectDirectory.getTestRunsDirectory().getPath().getFileName().toString();
                        VirtualFile trdDir = projectDir.createChildDirectory(this, trdName);
                        trdDir.createChildData(this, DirectoryType.TRD.getMarker());

                        projectDir.refresh(false, true);
                        projectPanel.getTestProjectSelector().addTestProject(newTestProjectDirectory);

                        Notifier.info("New Test Project", String.format("Test Project %s has been added", processedName));
                    });
                }
        ).show();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        execute();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (e.getProject() == null || Config.getTestinPath() == null) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}