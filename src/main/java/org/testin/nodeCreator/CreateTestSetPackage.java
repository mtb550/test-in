package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import org.testin.enums.DirectoryType;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.TreeUtilImpl;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSetPackage implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetPackageDirectoryDto tsp = Services.getInstance(project, DirectoryMapper.class).getTestSetPackageNode(project, newDirPath, parentDir);

        // The indexer owns all file/dir I/O: it creates the directory + .tsp marker
        // (with JSON content) and registers the node.
        Services.getInstance(project, ProjectIndexer.class).addTestSetPackage(tsp);

        TreeUtilImpl util = Services.getInstance(project, TreeUtilImpl.class);
        util.createNode(action.getTree(), parentNode, tsp);

        return tsp;
    }
}
