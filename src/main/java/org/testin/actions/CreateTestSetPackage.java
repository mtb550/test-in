package org.testin.actions;

import com.intellij.openapi.project.Project;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSetPackage implements DirectoryType.NodeCreator {

    @Override
    public void execute(final CreateTestNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetPackageDirectoryDto newTestSetPackageDirectory = new TestSetPackageDirectoryDto()
                .setName(name)
                .setPath(parentDir.getPath().resolve(name));

        TreeUtilImpl.createVf(this, parentDir.getPath(), name);
        TreeUtilImpl.createNode(action.getTree(), parentNode, newTestSetPackageDirectory);
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TSP.getMarker());

        Tools.createJavaPackageInTestRoot(project, name);
    }
}