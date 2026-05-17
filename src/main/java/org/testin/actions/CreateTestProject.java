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
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.util.TreeUtilImpl;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.notifications.Notifier;

import java.nio.file.Path;

public class CreateTestProject extends DumbAwareAction {
    private final ProjectPanel projectPanel;

    public CreateTestProject(final ProjectPanel projectPanel) {
        super("New Test Project", "Create a new test project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
    }

    private String extractProjectNameFromUrl(String gitUrl) {
        String name = gitUrl;

        // Remove trailing slash if present
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }

        // Remove .git extension if present
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }

        // Find the last slash (works for https) or colon (works for ssh)
        int lastSlashIndex = name.lastIndexOf('/');
        int lastColonIndex = name.lastIndexOf(':');

        int splitIndex = Math.max(lastSlashIndex, lastColonIndex);

        if (splitIndex != -1 && splitIndex < name.length() - 1) {
            return name.substring(splitIndex + 1);
        }

        return "ImportedTestProject"; // Fallback if URL is malformed
    }

    public void execute() {
        new CreateNodesDialog(CreateNodeMenu.TEST_PROJECT, (name, directoryType, codeGenerator) -> {
            if (name == null || name.trim().isEmpty()) return;

            if (directoryType == DirectoryType.IMPORT_TP) {
                String gitUrl = name.trim();
                String projectName = extractProjectNameFromUrl(gitUrl);

                new CloneProject(gitUrl, projectName, Config.getTestinPath()).execute();
                return;
            }


            // todo, cover all regex -> dots, slashes ..etc
            String processedName = name.replace("_", " ");

            TestProjectDirectoryDto newTestProjectDirectory = new TestProjectDirectoryDto().setName(processedName);

            String folderName = newTestProjectDirectory.getName();
            Path projectPath = Config.getTestinPath().resolve(folderName);
            newTestProjectDirectory.setPathName(folderName).setPath(projectPath);

            newTestProjectDirectory
                    .setTestCasesDirectory(new TestCasesMainDirectoryDto()
                            .setPath(newTestProjectDirectory.getPath().resolve(DirectoryType.TCD.getPathName()))
                            .setName(DirectoryType.TCD.getDisplayedName()))

                    .setTestRunsDirectory(new TestRunsMainDirectoryDto()
                            .setPath(newTestProjectDirectory.getPath().resolve(DirectoryType.TRD.getPathName()))
                            .setName(DirectoryType.TRD.getDisplayedName()));

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

                Notifier.getInstance().info("New Test Project", String.format("Test Project %s has been added", processedName));

                if (codeGenerator.isSelected()) {
                    GeneratorType.CREATE_TEST_PROJECT.getAction().execute(Config.getProject(), processedName, null);
                }
            });
        }
        ).show();
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
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