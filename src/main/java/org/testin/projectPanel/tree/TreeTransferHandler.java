package org.testin.projectPanel.tree;

import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TreeTransferHandler extends TransferHandler {
    public static final DataFlavor NODE_FLAVOR;

    static {
        try {
            NODE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + DefaultMutableTreeNode[].class.getName() + "\"");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to create custom DataFlavor", e);
        }
    }

    private final SimpleTree tree;
    @Getter
    private final Set<DefaultMutableTreeNode> selectedNodes;
    private Integer lastAction;

    public TreeTransferHandler(final SimpleTree tree, final Set<DefaultMutableTreeNode> selectedNodes) {
        this.tree = tree;
        this.selectedNodes = selectedNodes;
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(final JComponent c) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return null;

        DefaultMutableTreeNode[] nodes = Arrays.stream(paths)
                .map(path -> (DefaultMutableTreeNode) path.getLastPathComponent())
                .toArray(DefaultMutableTreeNode[]::new);

        return new NodesTransferable(nodes);
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) return false;

        if (support.isDrop()) {
            TreePath dropPath = ((SimpleTree.DropLocation) support.getDropLocation()).getPath();
            return dropPath != null && ((DefaultMutableTreeNode) dropPath.getLastPathComponent()).getUserObject() instanceof DirectoryDto;
        }
        return tree.getSelectionPath() != null;
    }

    @Override
    public boolean importData(final TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            final DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) support.getTransferable().getTransferData(NODE_FLAVOR);

            final DefaultMutableTreeNode targetNode = support.isDrop()
                    ? (DefaultMutableTreeNode) ((SimpleTree.DropLocation) support.getDropLocation()).getPath().getLastPathComponent()
                    : (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

            if (targetNode == null || !(targetNode.getUserObject() instanceof DirectoryDto targetDir)) return false;

            int action = support.isDrop() ? support.getDropAction() : (this.lastAction != null ? this.lastAction : COPY);

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            for (DefaultMutableTreeNode node : nodes) {
                if (action == MOVE) {

                    if (node.equals(targetNode) || node.isNodeDescendant(targetNode)) continue;
                    model.removeNodeFromParent(node);
                    persistMove(node, targetDir);
                    model.insertNodeInto(node, targetNode, targetNode.getChildCount());

                } else {
                    DefaultMutableTreeNode clone = deepCloneNode(node, targetDir.getPath());
                    persistCopy((DirectoryDto) node.getUserObject(), targetDir);
                    model.insertNodeInto(clone, targetNode, targetNode.getChildCount());
                }
            }

            resetLastAction();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private DefaultMutableTreeNode deepCloneNode(final DefaultMutableTreeNode node, final Path newParentPath) {
        Object userObject = node.getUserObject();

        if (!(userObject instanceof DirectoryDto dir)) {
            return new DefaultMutableTreeNode(userObject);
        }

        try {
            DirectoryDto clonedDir = dir.getClass().getDeclaredConstructor().newInstance();
            clonedDir.setName(dir.getName());

            Path newPath = newParentPath.resolve(dir.getName());
            clonedDir.setPath(newPath);

            DefaultMutableTreeNode clonedNode = new DefaultMutableTreeNode(clonedDir);

            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                clonedNode.add(deepCloneNode(child, newPath));
            }

            return clonedNode;

        } catch (Exception e) {
            System.err.println("Failed to deep clone node: " + e.getMessage());
            e.printStackTrace(System.err);
            return new DefaultMutableTreeNode(userObject);
        }
    }

    private void persistMove(final DefaultMutableTreeNode movedNode, final DirectoryDto targetDir) {
        final DirectoryDto sourceDir = (DirectoryDto) movedNode.getUserObject();

        TreeUtilImpl.executeVfsAction(sourceDir.getPath(), targetDir.getPath(), "Move Failed", (sourceVf, targetVf) -> {
            sourceVf.move(this, targetVf);

            Path oldPath = sourceDir.getPath();
            Path newPath = targetDir.getPath().resolve(sourceDir.getName());

            sourceDir.setPath(newPath);

            Tools.getInstance().updateChildrenPathsRecursive(movedNode, oldPath, newPath);

            System.out.println("Moved successfully to: " + newPath);
        });
    }

    private void persistCopy(final DirectoryDto source, final DirectoryDto target) {
        TreeUtilImpl.executeVfsAction(source.getPath(), target.getPath(), "Copy Failed", (sourceVf, targetVf) -> {
            sourceVf.copy(this, targetVf, sourceVf.getName());
            System.out.println("Copied successfully to: " + target.getPath().resolve(source.getName()));
        });
    }

    @Override
    protected void exportDone(final JComponent source, final Transferable data, int action) {
        resetLastAction();
    }

    public void resetLastAction() {
        this.lastAction = null;
        selectedNodes.clear();
        tree.repaint();
    }

    @Override
    public void exportToClipboard(final JComponent comp, final Clipboard clip, final int action) throws IllegalStateException {
        super.exportToClipboard(comp, clip, action);
        this.lastAction = action;

        selectedNodes.clear();
        if (action == MOVE) {
            TreePath[] paths = tree.getSelectionPaths();
            if (paths != null) {
                for (TreePath path : paths) {
                    selectedNodes.add((DefaultMutableTreeNode) path.getLastPathComponent());
                }
            }
        }
        tree.repaint();
    }

    private record NodesTransferable(DefaultMutableTreeNode[] nodes) implements Transferable {
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{NODE_FLAVOR, DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor) {
            return NODE_FLAVOR.equals(flavor) || DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public @NotNull Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {
            if (NODE_FLAVOR.equals(flavor)) {
                return nodes;
            }

            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                List<File> files = new ArrayList<>();
                for (DefaultMutableTreeNode node : nodes) {
                    if (node.getUserObject() instanceof DirectoryDto dirDto) {

                        if (dirDto instanceof TestSetDirectoryDto || dirDto instanceof TestRunDirectoryDto) {

                            Path nioPath = dirDto.getPath();
                            if (nioPath != null) {
                                files.add(nioPath.toFile());
                            }
                        }
                    }
                }
                return files;
            }

            throw new UnsupportedFlavorException(flavor);
        }
    }
}