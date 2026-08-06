package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jspecify.annotations.NonNull;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSetPackage implements NodeCreator {

    @Override
    public @NonNull DirectoryDto execute(final @NonNull SimpleTree tree, final @NonNull Project project, final @NonNull String name, final @NonNull DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final @NonNull Path newDirPath) {
        TestSetPackageDirectoryDto tsp = Services.getInstance(project, DirectoryMapper.class).getTestSetPackageNode(project, newDirPath, parentDir);

        Services.getInstance(project, ProjectIndexer.class).addTestSetPackage(tsp);
        Services.getInstance(project, ProjectIndexer.class).createNode(tree, parentNode, tsp);

        return tsp;
    }
}
