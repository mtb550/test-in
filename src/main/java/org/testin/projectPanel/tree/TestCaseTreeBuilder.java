package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.testin.pojo.DirectoryMapper;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestCaseTreeBuilder extends AbstractTreeBuilder {

    public TestCaseTreeBuilder(final @NotNull Project project, final ProjectPanel projectPanel) {
        super(project, projectPanel);
    }

    public void buildTree(final TestProjectDirectoryDto selectedTestProjectDirectory) {
        if (selectedTestProjectDirectory == null || selectedTestProjectDirectory.getMarker().getStatus() != ProjectStatus.ACTIVE) {
            this.rootNode = null;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (projectPanel.getProjectTree() != null) {
                    projectPanel.getProjectTree().updateNodes();
                }
            });
            return;
        }
        super.buildTree(selectedTestProjectDirectory.getTestCasesDirectory());
    }

    @Override
    protected DirectoryDto mapPathToDirectory(final Path path, DirectoryDto parentDir) {
        if (Files.exists(path.resolve(DirectoryType.TSP.getMarker())))
            return DirectoryMapper.getInstance().testSetPackageNode(project, path, parentDir);

        if (Files.exists(path.resolve(DirectoryType.TS.getMarker())))
            return DirectoryMapper.getInstance().testSetNode(project, path, parentDir);

        return null;
    }
}