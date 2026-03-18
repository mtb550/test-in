package testGit.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.tree.dirs.Directory;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AbstractTreeBuilder {
    protected final ProjectPanel projectPanel;

    @Getter
    protected DefaultMutableTreeNode rootNode;

    public AbstractTreeBuilder(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.rootNode = new DefaultMutableTreeNode("loading..");
    }

    public void buildTree(Directory rootDirectory) {
        DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(rootDirectory);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Path rootPath = rootDirectory.getPath();

            if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {
                try (Stream<Path> paths = Files.list(rootPath)) {
                    paths.map(this::mapPathToDirectory) // سيتم استدعاء الدالة من الكلاس الابن
                            .filter(Objects::nonNull)
                            .forEachOrdered(dir -> localRoot.add(buildNodeRecursive(dir)));

                } catch (Exception e) {
                    System.err.println("Failed to read directory: " + e.getMessage());
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

    protected abstract Directory mapPathToDirectory(Path path);
}
