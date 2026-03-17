package testGit.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.TestProject;
import testGit.pojo.TestRunsDirectory;
import testGit.pojo.mappers.TestRunMapper;
import testGit.pojo.mappers.TestRunPackageMapper;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class TestRunTabController {
    private final ProjectPanel projectPanel;
    @Getter
    private DefaultMutableTreeNode rootNode;

    public TestRunTabController(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.rootNode = new DefaultMutableTreeNode("loading..");
    }

    public void buildTreeAsync(TestProject selectedTestProject) {
        TestRunsDirectory trd = selectedTestProject.getTestRunsDirectory();
        DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(trd);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (Files.exists(trd.getPath()) && Files.isDirectory(trd.getPath())) {

                try (Stream<Path> paths = Files.list(trd.getPath())) {
                    paths.map(this::mapPathToDirectory)
                            .filter(Objects::nonNull)
                            .forEachOrdered(runDir -> localRoot.add(buildNodeRecursive(runDir)));

                } catch (Exception e) {
                    System.err.println("Failed to read test runs directory: " + e.getMessage());
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
        if (Files.exists(path.resolve(".trp"))) return TestRunPackageMapper.map(path);
        if (Files.exists(path.resolve(".tr"))) return TestRunMapper.map(path);
        return null;
    }
}