package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.generateJavaCode.clazz.GenerateJavaClass;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.util.EditorUtil;
import org.testin.util.Tools;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.nio.file.Path;

public class CreateTestSet implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final @NotNull Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {

        TestSetDirectoryDto ts = Services.getInstance(project, DirectoryMapper.class).getTestSetNode(project, newDirPath, parentDir);

        Services.getInstance(project, ProjectIndexer.class).addTestSet(ts);
        Services.getInstance(project, ProjectIndexer.class).createNode(action.getTree(), parentNode, ts);

        new GenerateJavaClass().create(project, parentDir.getName(), name);
        Services.getInstance(project, EditorUtil.class).open(project, ts);

        return ts;
    }

    public VirtualFile execute(final @NotNull Project project, final Object requestor, final VirtualFile targetDirectory, final DirectoryDto parentDirDto, final DefaultMutableTreeNode parentNode, final SimpleTree tree, final @NotNull String name) {
        String cName = Services.getInstance(project, Tools.class).removeSpecialChars(name);

        VirtualFile sheetDir = targetDirectory.findChild(cName);
        boolean isNewDirCreated = false;

        if (sheetDir == null) {
            try {
                sheetDir = targetDirectory.createChildDirectory(requestor, cName);

            } catch (final IOException ex) {
                Logger.error("Can't create directory: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
            isNewDirCreated = true;

            TestSetDirectoryDto newTsDto = Services.getInstance(project, DirectoryMapper.class).setTestSetNode(project, Path.of(sheetDir.getPath()), parentDirDto);
            Services.getInstance(project, ProjectIndexer.class).addTestSet(newTsDto);
            Services.getInstance(project, ProjectIndexer.class).createNode(tree, parentNode, newTsDto);
            new GenerateJavaClass().create(project, parentDirDto.getName(), cName);
        }

        if (sheetDir.findChild(DirectoryType.TS.getMarker()) == null) {
            try {
                sheetDir.createChildData(requestor, DirectoryType.TS.getMarker());
            } catch (final IOException ex) {
                Logger.error("Can't create directory: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        }

        if (isNewDirCreated && tree != null && tree.getModel() instanceof DefaultTreeModel treeModel) {
            treeModel.reload(parentNode);
            tree.updateUI();
            tree.revalidate();
        }

        return sheetDir;
    }

}