package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.*;
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

        TestProject newTestProject = new TestProject()
                .setProjectStatus(ProjectStatus.AC)
                .setName(name);

        String folderName = String.format("%s_%s", newTestProject.getName(), newTestProject.getProjectStatus());
        Path projectPath = Config.getTestGitPath().resolve(folderName);

        newTestProject.setFileName(folderName)
                .setFilePath(projectPath)
                .setFile(projectPath.toFile());

        newTestProject.setIcon(DirectoryIcon.PR);
        newTestProject.setTestCase(
                        new TestPackage()
                                .setIcon(DirectoryIcon.TCP)
                                .setFileName("TCP_testCases")
                                .setType(DirectoryType.TCP)
                                .setFile(newTestProject.getFile().toPath().resolve("TCP_testCases").toFile())
                                .setFilePath(newTestProject.getFile().toPath().resolve("TCP_testCases"))
                                .setName("Test Cases")
                )
                .setTestRun(
                        new TestPackage()
                                .setIcon(DirectoryIcon.TRP)
                                .setFileName("TRP_testRuns")
                                .setType(DirectoryType.TRP)
                                .setFile(newTestProject.getFile().toPath().resolve("TRP_testRuns").toFile())
                                .setFilePath(newTestProject.getFile().toPath().resolve("TRP_testRuns"))
                                .setName("Test Runs")
                );

        TreeUtilImpl.executeVfsAction(Config.getTestGitPath(), "IO Error", vf -> {
            VirtualFile projectDir = vf.createChildDirectory(this, folderName);

            projectDir.createChildDirectory(this, "TCP_testCases");
            projectDir.createChildDirectory(this, "TRP_testRuns");
            projectDir.refresh(false, true);

            projectPanel.getTestProjectSelector().addTestProject(newTestProject);

            Notifier.info("New Test Project", String.format("Test Project %s has been added", name));
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}