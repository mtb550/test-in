package testGit.projectPanel.tree;

import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.tree.dirs.Directory;
import testGit.util.Tools;
import testGit.util.TreeUtilImpl;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

public class TreeTransferHandler extends TransferHandler {
    private static final DataFlavor NODE_FLAVOR;

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

    public TreeTransferHandler(SimpleTree tree, Set<DefaultMutableTreeNode> selectedNodes) {
        this.tree = tree;
        this.selectedNodes = selectedNodes;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return null;

        DefaultMutableTreeNode[] nodes = Arrays.stream(paths)
                .map(path -> (DefaultMutableTreeNode) path.getLastPathComponent())
                .toArray(DefaultMutableTreeNode[]::new);

        return new NodesTransferable(nodes);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) return false;

        if (support.isDrop()) {
            TreePath dropPath = ((SimpleTree.DropLocation) support.getDropLocation()).getPath();
            return dropPath != null && ((DefaultMutableTreeNode) dropPath.getLastPathComponent()).getUserObject() instanceof Directory;
        }
        return tree.getSelectionPath() != null;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) support.getTransferable().getTransferData(NODE_FLAVOR);
            DefaultMutableTreeNode targetNode = support.isDrop()
                    ? (DefaultMutableTreeNode) ((SimpleTree.DropLocation) support.getDropLocation()).getPath().getLastPathComponent()
                    : (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

            if (targetNode == null || !(targetNode.getUserObject() instanceof Directory targetDir)) return false;

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
                    persistCopy((Directory) node.getUserObject(), targetDir);
                    model.insertNodeInto(clone, targetNode, targetNode.getChildCount());
                }
            }

            resetLastAction();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private DefaultMutableTreeNode deepCloneNode(DefaultMutableTreeNode node, Path newParentPath) {
        Object userObject = node.getUserObject();

        if (!(userObject instanceof Directory dir)) {
            return new DefaultMutableTreeNode(userObject);
        }

        try {
            // استنساخ الكائن
            Directory clonedDir = dir.getClass().getDeclaredConstructor().newInstance();
            clonedDir.setName(dir.getName());

            // 🌟 حساب المسار الجديد لهذه النسخة وتعيينه
            Path newPath = newParentPath.resolve(dir.getName());
            clonedDir.setPath(newPath);

            DefaultMutableTreeNode clonedNode = new DefaultMutableTreeNode(clonedDir);

            // 🌟 استدعاء ذاتي (Recursion) لنسخ كل الأبناء!
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

    private void persistMove(DefaultMutableTreeNode movedNode, Directory targetDir) {
        Directory sourceDir = (Directory) movedNode.getUserObject();

        TreeUtilImpl.executeVfsAction(sourceDir.getPath(), targetDir.getPath(), "Move Failed", (sourceVf, targetVf) -> {
            sourceVf.move(this, targetVf);

            Path oldPath = sourceDir.getPath();
            Path newPath = targetDir.getPath().resolve(sourceDir.getName());

            sourceDir.setPath(newPath);

            Tools.updateChildrenPathsRecursive(movedNode, oldPath, newPath);

            System.out.println("Moved successfully to: " + newPath);
        });
    }

    private void persistCopy(Directory source, Directory target) {
        TreeUtilImpl.executeVfsAction(source.getPath(), target.getPath(), "Copy Failed", (sourceVf, targetVf) -> {
            sourceVf.copy(this, targetVf, sourceVf.getName());
            System.out.println("Copied successfully to: " + target.getPath().resolve(source.getName()));
        });
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        resetLastAction();
    }

    public void resetLastAction() {
        this.lastAction = null;
        selectedNodes.clear();
        tree.repaint();
    }

    @Override
    public void exportToClipboard(JComponent comp, java.awt.datatransfer.Clipboard clip, int action) throws IllegalStateException {
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
            return new DataFlavor[]{NODE_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return NODE_FLAVOR.equals(flavor);
        }

        @Override
        public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return nodes;
        }
    }
}