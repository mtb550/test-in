package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractTreeBuilder {
    protected final Project project;
    protected final ProjectPanel projectPanel;

    @Getter
    protected DefaultMutableTreeNode rootNode;

    public AbstractTreeBuilder(final @NotNull Project project, final ProjectPanel projectPanel) {
        this.project = project;
        this.projectPanel = projectPanel;
        this.rootNode = new DefaultMutableTreeNode("loading..");
    }

    public void buildTree(final DirectoryDto rootDirectoryDto) {
        DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(rootDirectoryDto);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
            indexer.awaitIndexing();

            if (projectPanel.getProject().isDisposed()) return;

            final Path rootPath = rootDirectoryDto.getPath();

            final List<DirectoryDto> children = getChildrenFromIndexer(rootPath);

            for (final DirectoryDto child : children) {
                localRoot.add(buildNodeFromIndexer(child));
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                this.rootNode = localRoot;
                if (projectPanel.getProjectTree() != null) {
                    projectPanel.getProjectTree().updateNodes();
                }
            });
        });
    }

    private DefaultMutableTreeNode buildNodeFromIndexer(final @NotNull DirectoryDto currentDir) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(currentDir);
        final Path currentPath = currentDir.getPath();

        final List<DirectoryDto> children = getChildrenFromIndexer(currentPath);
        for (final DirectoryDto child : children) {
            node.add(buildNodeFromIndexer(child));
        }

        return node;
    }

    private List<DirectoryDto> getChildrenFromIndexer(final Path path) {
        final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
        return indexer.getChildren(path);
    }

}