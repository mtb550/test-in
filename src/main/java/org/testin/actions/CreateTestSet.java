package org.testin.actions;

import com.intellij.openapi.project.Project;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.NodeCreator;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestSet implements NodeCreator {

    @Override
    public void execute(final CreateTestNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetDirectoryDto newTestSetDirectory = new TestSetDirectoryDto()
                .setName(name)
                .setPath(parentDir.getPath().resolve(name));

        TreeUtilImpl.createVf(this, parentDir.getPath(), newTestSetDirectory.getName());
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TS.getMarker());
        TreeUtilImpl.createNode(action.getTree(), parentNode, newTestSetDirectory);

        Tools.getInstance().createJavaClassInTestRoot(project, parentDir.getName(), name);
        Tools.getInstance().openTestEditor(newTestSetDirectory);
    }
}