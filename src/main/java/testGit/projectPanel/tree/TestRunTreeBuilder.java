package testGit.projectPanel.tree;

import testGit.pojo.DirectoryType;
import testGit.pojo.tree.dirs.Directory;
import testGit.pojo.tree.dirs.TestProjectDirectory;
import testGit.pojo.tree.mappers.TestRunMapper;
import testGit.pojo.tree.mappers.TestRunPackageMapper;
import testGit.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestRunTreeBuilder extends AbstractTreeBuilder {

    public TestRunTreeBuilder(ProjectPanel projectPanel) {
        super(projectPanel);
    }

    public void buildTree(TestProjectDirectory selectedTestProjectDirectory) {
        super.buildTree(selectedTestProjectDirectory.getTestRunsDirectory());
    }

    @Override
    protected Directory mapPathToDirectory(Path path) {
        if (Files.exists(path.resolve(DirectoryType.TRP.getMarker()))) return TestRunPackageMapper.map(path);
        if (Files.exists(path.resolve(DirectoryType.TR.getMarker()))) return TestRunMapper.map(path);
        return null;
    }
}