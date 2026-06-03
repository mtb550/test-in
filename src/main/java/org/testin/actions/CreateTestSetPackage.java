package org.testin.actions;

import com.intellij.openapi.project.Project;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.NodeCreator;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSetPackage implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTestNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetPackageDirectoryDto tsp = DirectoryMapper.getInstance().testSetPackageNode(newDirPath, parentDir);

        TreeUtilImpl.createVf(project, this, parentDir.getPath(), name);
        TreeUtilImpl.createNode(action.getTree(), parentNode, tsp);
        TreeUtilImpl.createDataVf(project, this, newDirPath, DirectoryType.TSP.getMarker());

        return tsp;
    }
}