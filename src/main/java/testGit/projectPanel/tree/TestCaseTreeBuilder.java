package testGit.projectPanel.tree;

import testGit.pojo.DirectoryMapper;
import testGit.pojo.DirectoryType;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
import testGit.projectPanel.ProjectPanel;

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
    protected DirectoryDto mapPathToDirectory(final Path path) {
        if (Files.exists(path.resolve(DirectoryType.TSP.getMarker()))) return DirectoryMapper.testSetPackageNode(path);
        if (Files.exists(path.resolve(DirectoryType.TS.getMarker()))) return DirectoryMapper.testSetNode(path);
        return null;
    }
}