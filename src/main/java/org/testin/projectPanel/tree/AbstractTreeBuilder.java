package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.logger.Log;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AbstractTreeBuilder {
    protected final ProjectPanel projectPanel;

    @Getter
    protected DefaultMutableTreeNode rootNode;

    public AbstractTreeBuilder(final ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.rootNode = new DefaultMutableTreeNode("loading..");
    }

    public void buildTree(final DirectoryDto rootDirectoryDto) {
        DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(rootDirectoryDto);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Path rootPath = rootDirectoryDto.getPath();

            if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {
                try (Stream<Path> paths = Files.list(rootPath)) {
                    paths.map(path -> mapPathToDirectory(path, rootDirectoryDto))
                            .filter(Objects::nonNull)
                            .forEachOrdered(dir -> localRoot.add(buildNodeRecursive(dir)));

                } catch (Exception e) {
                    Log.error("Failed to read directory: " + e.getMessage());
                    Log.error("Exception: " + e.getMessage());
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

    private DefaultMutableTreeNode buildNodeRecursive(@NotNull final DirectoryDto currentDir) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(currentDir);
        Path currentPath = currentDir.getPath();

        if (Files.exists(currentPath) && Files.isDirectory(currentPath)) {
            try (Stream<Path> paths = Files.list(currentPath)) {
                paths.map(path -> mapPathToDirectory(path, currentDir))
                        .filter(Objects::nonNull)
                        .forEachOrdered(childDir -> node.add(buildNodeRecursive(childDir)));

            } catch (Exception e) {
                Log.error("Failed to read directory recursively: " + currentPath);
                Log.error("Exception: " + e.getMessage());
            }
        }
        return node;
    }

    protected abstract DirectoryDto mapPathToDirectory(final Path path, final DirectoryDto parentDir);
}