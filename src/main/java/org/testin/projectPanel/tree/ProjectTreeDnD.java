package org.testin.projectPanel.tree;

import com.intellij.ide.dnd.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.util.TreeUtilImpl;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectTreeDnD {

    public static final DataFlavor DTO_LIST_FLAVOR;

    static {
        try {
            DTO_LIST_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"java.util.List\"");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to create custom DataFlavor", e);
        }
    }

    public static void install(@NotNull SimpleTree tree, @NotNull Project project, @NotNull StructureTreeModel<?> structureModel) {
        DnDManager manager = DnDManager.getInstance();
        manager.registerSource(new TreeSource(tree), tree);
        manager.registerTarget(new TreeTarget(project, tree, structureModel), tree);
    }

    private static DirectoryNode extractNode(TreePath path) {
        if (path == null) return null;
        Object userObject = TreeUtil.getUserObject(path.getLastPathComponent());
        if (userObject instanceof DirectoryNode) {
            return (DirectoryNode) userObject;
        }
        return null;
    }

    public record DtoTransferable(List<DirectoryDto> dtos) implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DTO_LIST_FLAVOR, DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DTO_LIST_FLAVOR.equals(flavor) || DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @NotNull
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (DTO_LIST_FLAVOR.equals(flavor)) {
                return dtos;
            }

            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                List<File> files = new ArrayList<>();
                for (DirectoryDto dto : dtos) {
                    files.add(dto.getPath().toFile());
                }
                return files;
            }

            throw new UnsupportedFlavorException(flavor);
        }
    }

    private record TreeSource(SimpleTree tree) implements DnDSource {

        @Override
        public boolean canStartDragging(DnDAction action, @NotNull Point dragOrigin) {
            return tree.getSelectionPaths() != null && tree.getSelectionPaths().length > 0;
        }

        @Override
        public DnDDragStartBean startDragging(DnDAction action, @NotNull Point dragOrigin) {
            List<DirectoryDto> draggedData = new ArrayList<>();
            TreePath[] paths = tree.getSelectionPaths();

            if (paths != null) {
                for (TreePath path : paths) {
                    DirectoryNode node = extractNode(path);
                    if (node != null && node.getValue() != null) {
                        draggedData.add(node.getValue());
                    }
                }
            }

            return new DnDDragStartBean(new DtoTransferable(draggedData));
        }
    }

    private record TreeTarget(Project project, SimpleTree tree,
                              StructureTreeModel<?> structureModel) implements DnDTarget {

        @Override
        public boolean update(DnDEvent event) {
            event.setDropPossible(false);

            if (!(event.getAttachedObject() instanceof DtoTransferable)) return false;

            TreePath dropPath = tree.getPathForLocation(event.getPoint().x, event.getPoint().y);
            if (dropPath == null) return false;

            DirectoryNode targetNode = extractNode(dropPath);
            if (targetNode == null || targetNode.getValue() == null) return false;

            event.setDropPossible(true);
            return true;
        }

        @Override
        public void drop(DnDEvent event) {
            if (!(event.getAttachedObject() instanceof DtoTransferable(List<DirectoryDto> draggedDirs))) return;

            TreePath dropPath = tree.getPathForLocation(event.getPoint().x, event.getPoint().y);
            if (dropPath == null) return;

            DirectoryNode targetNode = extractNode(dropPath);
            if (targetNode == null) return;

            DirectoryDto targetDir = targetNode.getValue();
            boolean isMove = event.getAction() != null && event.getAction().getActionId() == DnDConstants.ACTION_MOVE;

            for (DirectoryDto sourceDir : draggedDirs) {
                if (isMove) {
                    persistMove(sourceDir, targetDir);
                } else {
                    persistCopy(sourceDir, targetDir);
                }
            }

            structureModel.invalidateAsync();
        }

        private void persistMove(DirectoryDto sourceDir, DirectoryDto targetDir) {
            Services.getInstance(project, TreeUtilImpl.class).executeVfsAction(project, sourceDir.getPath(), targetDir.getPath(), "Move Failed", (sourceVf, targetVf) -> {
                sourceVf.move(this, targetVf);
                Path newPath = targetDir.getPath().resolve(sourceDir.getName());
                sourceDir.setPath(newPath);
                Log.info("Moved successfully to: " + newPath);
            });
        }

        private void persistCopy(DirectoryDto source, DirectoryDto target) {
            Services.getInstance(project, TreeUtilImpl.class).executeVfsAction(project, source.getPath(), target.getPath(), "Copy Failed", (sourceVf, targetVf) -> {
                sourceVf.copy(this, targetVf, sourceVf.getName());
                Log.info("Copied successfully to: " + target.getPath().resolve(source.getName()));
            });
        }
    }
}