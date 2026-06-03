package org.testin.actions;

import com.intellij.openapi.project.Project;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.NodeCreator;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestRunPackage implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTestNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestRunPackageDirectoryDto tr = DirectoryMapper.getInstance().testRunPackageNode(project, newDirPath, parentDir);

        TreeUtilImpl.createVf(project, this, parentDir.getPath(), name);
        TreeUtilImpl.createNode(action.getTree(), parentNode, tr);
        TreeUtilImpl.createDataVf(project, this, newDirPath, DirectoryType.TRP.getMarker());

        return tr;
    }
}