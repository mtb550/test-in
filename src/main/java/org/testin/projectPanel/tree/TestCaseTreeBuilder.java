package org.testin.projectPanel.tree;

import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestCaseTreeBuilder extends AbstractTreeBuilder {

    public TestCaseTreeBuilder(final ProjectPanel projectPanel) {
        super(projectPanel);
    }

    public void buildTree(final TestProjectDirectoryDto selectedTestProjectDirectory) {
        super.buildTree(selectedTestProjectDirectory.getTestCasesDirectory());
    }

    @Override
    protected DirectoryDto mapPathToDirectory(final Path path, DirectoryDto parentDir) {
        if (Files.exists(path.resolve(DirectoryType.TSP.getMarker())))
            return DirectoryMapper.getInstance().testSetPackageNode(path, parentDir);

        if (Files.exists(path.resolve(DirectoryType.TS.getMarker())))
            return DirectoryMapper.getInstance().testSetNode(path, parentDir);

        return null;
    }
}