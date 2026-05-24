package org.testin.projectPanel.tree;

import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestRunTreeBuilder extends AbstractTreeBuilder {

    public TestRunTreeBuilder(final ProjectPanel projectPanel) {
        super(projectPanel);
    }

    public void buildTree(final TestProjectDirectoryDto selectedTestProjectDirectory) {
        super.buildTree(selectedTestProjectDirectory.getTestRunsDirectory());
    }

    @Override
    protected DirectoryDto mapPathToDirectory(final Path path, final DirectoryDto parentDir) {
        if (Files.exists(path.resolve(DirectoryType.TRP.getMarker())))
            return DirectoryMapper.getInstance().testRunPackageNode(path, parentDir);

        if (Files.exists(path.resolve(DirectoryType.TR.getMarker())))
            return DirectoryMapper.getInstance().testRunNode(path, parentDir);

        return null;
    }
}