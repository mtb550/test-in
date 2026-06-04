package org.testin.projectPanel.tree;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestProjectTreeBuilder extends AbstractTreeBuilder {

    public TestProjectTreeBuilder(final @NotNull Project project, final ProjectPanel projectPanel) {
        super(project, projectPanel);
    }

    @Override
    protected DirectoryDto mapPathToDirectory(final Path path, final DirectoryDto parentDir) {
        if (Files.exists(path.resolve(DirectoryType.TP.getMarker()))) {
            return DirectoryMapper.getInstance().testProjectNode(project, path);
        }

        return null;
    }
}