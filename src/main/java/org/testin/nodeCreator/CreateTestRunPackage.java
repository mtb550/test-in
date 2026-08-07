package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestRunPackage implements NodeCreator {
    private final @NotNull Project p;

    public CreateTestRunPackage(final @NotNull Project p) {
        this.p = p;
    }

    @Override
    public @NonNull DirectoryDto execute(final @NonNull SimpleTree tree, final @NonNull String name, final @NonNull DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final @NonNull Path newDirPath) {
        TestRunPackageDirectoryDto tr = Services.getInstance(p, DirectoryMapper.class).getTestRunPackageNode(p, newDirPath, parentDir);

        // The indexer owns all file/dir I/O: it creates the directory + .trp marker
        // (with JSON content) and registers the node.
        Services.getInstance(p, ProjectIndexer.class).addTestRunPackage(tr);

        // Tree-node insertion is routed through the indexer (TreeUtilImpl stays indexer-only).
        Services.getInstance(p, ProjectIndexer.class).createNode(tree, parentNode, tr);

        return tr;
    }
}
