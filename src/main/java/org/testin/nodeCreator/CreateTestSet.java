package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.testin.generateJavaCode.clazz.GenerateJavaClass;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSet implements NodeCreator {

    @Override
    public @NonNull DirectoryDto execute(final @NonNull SimpleTree tree, final @NotNull Project project, final @NonNull String name, final @NonNull DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final @NonNull Path newDirPath) {
        final TestSetDirectoryDto ts = Services.getInstance(project, DirectoryMapper.class).getTestSetNode(project, newDirPath, parentDir);

        Services.getInstance(project, ProjectIndexer.class).addTestSet(ts);
        Services.getInstance(project, ProjectIndexer.class).createNode(tree, parentNode, ts);

        new GenerateJavaClass().create(project, parentDir.getName(), name);
        return ts;
    }
}
