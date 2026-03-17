package testGit.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestCasesDirectory;
import testGit.pojo.TestProject;
import testGit.pojo.mappers.TestSetMapper;
import testGit.pojo.mappers.TestSetPackageMapper;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class TestCaseTabController {
    private final ProjectPanel projectPanel;
    @Getter
    private DefaultMutableTreeNode rootNode;

    public TestCaseTabController(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.rootNode = new DefaultMutableTreeNode("loading..");
    }

    public void buildTreeAsync(TestProject selectedTestProject) {
        TestCasesDirectory tcd = selectedTestProject.getTestCasesDirectory();
        DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(tcd);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (Files.exists(tcd.getPath()) && Files.isDirectory(tcd.getPath())) {

                try (Stream<Path> paths = Files.list(tcd.getPath())) {
                    paths.map(this::mapPathToDirectory)
                            .filter(Objects::nonNull)
                            .forEachOrdered(caseDir -> localRoot.add(buildNodeRecursive(caseDir)));

                } catch (Exception e) {
                    System.err.println("Failed to read test cases directory: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                this.rootNode = localRoot;
                if (projectPanel.getProjectTree() != null) {
                    projectPanel.getProjectTree().updateNodes();
                }
            });
        });
    }

    private DefaultMutableTreeNode buildNodeRecursive(@NotNull Directory dir) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);
        Path currentPath = dir.getPath();

        if (Files.exists(currentPath) && Files.isDirectory(currentPath)) {

            try (Stream<Path> paths = Files.list(currentPath)) {
                paths.map(this::mapPathToDirectory)
                        .filter(Objects::nonNull)
                        .forEachOrdered(childDir -> node.add(buildNodeRecursive(childDir)));

            } catch (Exception e) {
                System.err.println("Failed to read directory recursively: " + currentPath);
                e.printStackTrace(System.err);
            }
        }

        return node;
    }

    private Directory mapPathToDirectory(Path path) {
        if (Files.exists(path.resolve(DirectoryType.TSP.getMarker()))) return TestSetPackageMapper.map(path);
        if (Files.exists(path.resolve(DirectoryType.TS.getMarker()))) return TestSetMapper.map(path);
        return null;
    }
}