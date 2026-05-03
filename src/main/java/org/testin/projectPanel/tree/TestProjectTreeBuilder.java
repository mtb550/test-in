package org.testin.projectPanel.tree;

import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestProjectTreeBuilder extends AbstractTreeBuilder {

    public TestProjectTreeBuilder(final ProjectPanel projectPanel) {
        super(projectPanel);
    }

    @Override
    protected DirectoryDto mapPathToDirectory(final Path path) {
        if (Files.exists(path.resolve(DirectoryType.TP.getMarker()))) {
            return DirectoryMapper.testProjectNode(path);
        }

        return null;
    }
}