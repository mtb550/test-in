package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSetPackage implements NodeCreator {
    private final @NotNull Project p;

    public CreateTestSetPackage(final @NotNull Project p) {
        this.p = p;
    }

    @Override
    public @NonNull DirectoryDto execute(final @NonNull SimpleTree tree, final @NonNull String name, final @NonNull DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final @NonNull Path newDirPath) {
        TestSetPackageDirectoryDto tsp = Services.getInstance(p, DirectoryMapper.class).getTestSetPackageNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSetPackage(tsp);
        Services.getInstance(p, ProjectIndexer.class).createNode(tree, parentNode, tsp);

        return tsp;
    }
}
