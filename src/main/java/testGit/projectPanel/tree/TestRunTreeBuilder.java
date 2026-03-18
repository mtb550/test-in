package testGit.projectPanel.tree;

import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestProject;
import testGit.pojo.mappers.TestRunMapper;
import testGit.pojo.mappers.TestRunPackageMapper;
import testGit.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestRunTreeBuilder extends AbstractTreeBuilder {

    public TestRunTreeBuilder(ProjectPanel projectPanel) {
        super(projectPanel);
    }

    public void buildTree(TestProject selectedTestProject) {
        super.buildTree(selectedTestProject.getTestRunsDirectory());
    }

    @Override
    protected Directory mapPathToDirectory(Path path) {
        if (Files.exists(path.resolve(DirectoryType.TRP.getMarker()))) return TestRunPackageMapper.map(path);
        if (Files.exists(path.resolve(DirectoryType.TR.getMarker()))) return TestRunMapper.map(path);
        return null;
    }
}