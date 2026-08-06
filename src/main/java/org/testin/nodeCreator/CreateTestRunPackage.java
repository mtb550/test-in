package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestRunPackage implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestRunPackageDirectoryDto tr = Services.getInstance(project, DirectoryMapper.class).getTestRunPackageNode(project, newDirPath, parentDir);

        // The indexer owns all file/dir I/O: it creates the directory + .trp marker
        // (with JSON content) and registers the node.
        Services.getInstance(project, ProjectIndexer.class).addTestRunPackage(tr);

        // Tree-node insertion is routed through the indexer (TreeUtilImpl stays indexer-only).
        Services.getInstance(project, ProjectIndexer.class).createNode(action.getTree(), parentNode, tr);

        return tr;
    }
}
