package org.testin.nodeCreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSet implements NodeCreator {
    private final @NotNull Project p;

    public CreateTestSet(final @NotNull Project p) {
        this.p = p;
    }

    @Override
    public @NonNull DirectoryDto execute(final @NonNull SimpleTree tree, final @NonNull String name, final @NonNull DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final @NonNull Path newDirPath) {
        final TestSetDirectoryDto ts = Services.getInstance(p, DirectoryMapper.class).getTestSetNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSet(ts);
        Services.getInstance(p, ProjectIndexer.class).createNode(tree, parentNode, ts);

        // Code generation is owned by the generateJavaCode package (accepted). CreateJavaClass
        // builds the FQCN/package from the test-set DTO and writes the class via VFS internally.
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                GeneratorType.CREATE_TEST_SET.getAction().execute(p, ts);
            } catch (final Exception ex) {
                Logger.error("Failed to create Java class: " + ex.getMessage());
            }
        });

        return ts;
    }

}

