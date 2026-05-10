package org.testin.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.testin.pojo.Config;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.NodeCreator;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
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

    public VirtualFile inBackground(final Object requestor, final VirtualFile targetDirectory, final DirectoryDto parentDirDto, final DefaultMutableTreeNode parentNode, final SimpleTree tree, final String name) throws IOException {
        String safeDirName = name.replaceAll("[\\\\/:*?\"<>|]", "_");

        VirtualFile sheetDir = targetDirectory.findChild(safeDirName);
        boolean isNewDirCreated = false;

        if (sheetDir == null) {
            sheetDir = targetDirectory.createChildDirectory(requestor, safeDirName);
            isNewDirCreated = true;

            TestSetDirectoryDto newTsDto = new TestSetDirectoryDto()
                    .setName(safeDirName)
                    .setPath(parentDirDto.getPath().resolve(safeDirName));

            TreeUtilImpl.createNode(tree, parentNode, newTsDto);
            Tools.getInstance().createJavaClassInTestRoot(Config.getProject(), parentDirDto.getName(), safeDirName);
        }

        if (sheetDir.findChild(".ts") == null) {
            sheetDir.createChildData(requestor, ".ts");
        }

        if (isNewDirCreated && tree != null && tree.getModel() instanceof DefaultTreeModel treeModel) {
            treeModel.reload(parentNode);
            tree.updateUI();
            tree.revalidate();
        }

        return sheetDir;
    }
}