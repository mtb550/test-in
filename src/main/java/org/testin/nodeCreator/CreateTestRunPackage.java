package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import org.testin.enums.DirectoryType;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.util.TreeUtilImpl;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestRunPackage implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestRunPackageDirectoryDto tr = Services.getInstance(project, DirectoryMapper.class).getTestRunPackageNode(project, newDirPath, parentDir);

        Services.getInstance(project, ProjectIndexer.class).addTestRunPackage(tr);

        TreeUtilImpl util = Services.getInstance(project, TreeUtilImpl.class);
        util.createVf(project, this, parentDir.getPath(), name);
        util.createNode(action.getTree(), parentNode, tr);
        util.createDataVf(project, this, newDirPath, DirectoryType.TRP.getMarker());

        return tr;
    }
}
