package org.testin.actions;

import com.intellij.openapi.project.Project;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.NodeCreator;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.util.TreeUtilImpl;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestRunPackage implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestRunPackageDirectoryDto tr = Services.getInstance(project, DirectoryMapper.class).readTestRunPackageNode(project, newDirPath, parentDir);

        TreeUtilImpl util = Services.getInstance(project, TreeUtilImpl.class);
        util.createVf(project, this, parentDir.getPath(), name);
        util.createNode(action.getTree(), parentNode, tr);
        util.createDataVf(project, this, newDirPath, DirectoryType.TRP.getMarker());

        return tr;
    }
}
