package org.testin.actions;

import com.intellij.openapi.project.Project;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.NodeCreator;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.TreeUtilImpl;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSetPackage implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetPackageDirectoryDto tsp = Services.getInstance(project, DirectoryMapper.class).readTestSetPackageNode(project, newDirPath, parentDir);

        TreeUtilImpl util = Services.getInstance(project, TreeUtilImpl.class);
        util.createVf(project, this, parentDir.getPath(), name);
        util.createNode(action.getTree(), parentNode, tsp);
        util.createDataVf(project, this, newDirPath, DirectoryType.TSP.getMarker());

        return tsp;
    }
}
