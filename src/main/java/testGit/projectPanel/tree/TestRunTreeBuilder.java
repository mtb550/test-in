package testGit.projectPanel.tree;

import testGit.pojo.DirectoryMapper;
import testGit.pojo.DirectoryType;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
import testGit.projectPanel.ProjectPanel;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestRunTreeBuilder extends AbstractTreeBuilder {

    public TestRunTreeBuilder(ProjectPanel projectPanel) {
        super(projectPanel);
    }

    public void buildTree(TestProjectDirectoryDto selectedTestProjectDirectory) {
        super.buildTree(selectedTestProjectDirectory.getTestRunsDirectory());
    }

    @Override
    protected DirectoryDto mapPathToDirectory(Path path) {
        if (Files.exists(path.resolve(DirectoryType.TRP.getMarker()))) return DirectoryMapper.testRunPackageNode(path);
        if (Files.exists(path.resolve(DirectoryType.TR.getMarker()))) return DirectoryMapper.testRunNode(path);
        return null;
    }
}