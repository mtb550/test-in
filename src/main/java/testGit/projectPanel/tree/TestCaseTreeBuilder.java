package testGit.projectPanel.tree;

import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestProject;
import testGit.pojo.mappers.TestSetMapper;
import testGit.pojo.mappers.TestSetPackageMapper;
import testGit.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestCaseTreeBuilder extends AbstractTreeBuilder {

    public TestCaseTreeBuilder(final ProjectPanel projectPanel) {
        super(projectPanel);
    }

    public void buildTree(final TestProject selectedTestProject) {
        super.buildTree(selectedTestProject.getTestCasesDirectory());
    }

    @Override
    protected Directory mapPathToDirectory(final Path path) {
        if (Files.exists(path.resolve(DirectoryType.TSP.getMarker()))) return TestSetPackageMapper.map(path);
        if (Files.exists(path.resolve(DirectoryType.TS.getMarker()))) return TestSetMapper.map(path);
        return null;
    }
}