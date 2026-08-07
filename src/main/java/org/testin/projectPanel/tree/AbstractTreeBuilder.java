package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractTreeBuilder {
    protected final @NotNull Project p;
    protected final ProjectPanel pp;

    @Getter
    protected DefaultMutableTreeNode rootNode;

    public AbstractTreeBuilder(final @NotNull Project p, final ProjectPanel pp) {
        this.p = p;
        this.pp = pp;
        this.rootNode = new DefaultMutableTreeNode("loading..");
    }

    public void buildTree(final DirectoryDto rootDirectoryDto) {
        DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(rootDirectoryDto);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                indexer.awaitIndexing();

                if (p.isDisposed()) return;

                final Path rootPath = rootDirectoryDto.getPath();

                final List<DirectoryDto> children = getChildrenFromIndexer(rootPath);

                for (final DirectoryDto child : children) {
                    localRoot.add(buildNodeFromIndexer(child));
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    this.rootNode = localRoot;
                    pp.getProjectTree().refreshTree();
                });

            } catch (final Exception ex) {
                Logger.error("AbstractTreeBuilder.buildTree() error for directory '" + (rootDirectoryDto != null ? rootDirectoryDto.getName() : "null") + "': " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    this.rootNode = null;
                    pp.getProjectTree().refreshTree();
                });
            }
        });
    }

    private DefaultMutableTreeNode buildNodeFromIndexer(final @NotNull DirectoryDto currentDir) {
        try {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(currentDir);
            final Path currentPath = currentDir.getPath();

            final List<DirectoryDto> children = getChildrenFromIndexer(currentPath);
            for (final DirectoryDto child : children) {
                node.add(buildNodeFromIndexer(child));
            }

            return node;

        } catch (final Exception ex) {
            Logger.error("buildNodeFromIndexer() error for directory '" + currentDir.getName() + "': " + ex.getMessage());
            return new DefaultMutableTreeNode(currentDir.getName());
        }
    }

    private List<DirectoryDto> getChildrenFromIndexer(final Path path) {
        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        return indexer.getChildren(path);
    }

}