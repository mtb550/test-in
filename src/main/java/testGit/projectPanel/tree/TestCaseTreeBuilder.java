package testGit.projectPanel.tree;

import testGit.pojo.DirectoryType;
import testGit.pojo.tree.dirs.Directory;
import testGit.pojo.tree.dirs.TestProjectDirectory;
import testGit.pojo.tree.mappers.TestSetMapper;
import testGit.pojo.tree.mappers.TestSetPackageMapper;
import testGit.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestCaseTreeBuilder extends AbstractTreeBuilder {

    public TestCaseTreeBuilder(final ProjectPanel projectPanel) {
        super(projectPanel);
    }

    public void buildTree(final TestProjectDirectory selectedTestProjectDirectory) {
        super.buildTree(selectedTestProjectDirectory.getTestCasesDirectory());
    }

    @Override
    protected Directory mapPathToDirectory(final Path path) {
        if (Files.exists(path.resolve(DirectoryType.TSP.getMarker()))) return TestSetPackageMapper.map(path);
        if (Files.exists(path.resolve(DirectoryType.TS.getMarker()))) return TestSetMapper.map(path);
        return null;
    }
}