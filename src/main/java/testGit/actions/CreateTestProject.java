package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.ProjectStatus;
import testGit.pojo.dto.dirs.TestCasesDirectoryDto;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
import testGit.pojo.dto.dirs.TestRunsDirectoryDto;
import testGit.projectPanel.ProjectPanel;
import testGit.ui.CreateTestProjectDialog;
import testGit.util.Notifier;
import testGit.util.TreeUtilImpl;

import java.nio.file.Path;

public class CreateTestProject extends DumbAwareAction {
    private final ProjectPanel projectPanel;

    public CreateTestProject(ProjectPanel projectPanel) {
        super("New Test Project", "Create a new test project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        String name = CreateTestProjectDialog.show();

        if (name == null || name.trim().isEmpty()) return;

        TestProjectDirectoryDto newTestProjectDirectory = new TestProjectDirectoryDto()
                .setProjectStatus(ProjectStatus.AC)
                .setName(name);

        String folderName = String.format("%s_%s", newTestProjectDirectory.getName(), newTestProjectDirectory.getProjectStatus());
        Path projectPath = Config.getTestGitPath().resolve(folderName);

        newTestProjectDirectory.setPathName(folderName)
                .setPath(projectPath);

        newTestProjectDirectory.setTestCasesDirectory(
                        new TestCasesDirectoryDto()
                                .setPath(newTestProjectDirectory.getPath().resolve("testCases"))
                                .setName("Test Cases")
                )
                .setTestRunsDirectory(
                        new TestRunsDirectoryDto()
                                .setPath(newTestProjectDirectory.getPath().resolve("testRuns"))
                                .setName("Test Runs")
                );

        TreeUtilImpl.executeVfsAction(Config.getTestGitPath(), "IO Error", vf -> {
            VirtualFile projectDir = vf.createChildDirectory(this, folderName);
            projectDir.createChildData(this, ".pr");

            String tcdName = newTestProjectDirectory.getTestCasesDirectory().getPath().getFileName().toString();
            VirtualFile tcdDir = projectDir.createChildDirectory(this, tcdName);
            tcdDir.createChildData(this, ".tcd");

            String trdName = newTestProjectDirectory.getTestRunsDirectory().getPath().getFileName().toString();
            VirtualFile trdDir = projectDir.createChildDirectory(this, trdName);
            trdDir.createChildData(this, ".trd");

            projectDir.refresh(false, true);
            projectPanel.getTestProjectSelector().addTestProject(newTestProjectDirectory);
            Notifier.info("New Test Project", String.format("Test Project %s has been added", name));
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}