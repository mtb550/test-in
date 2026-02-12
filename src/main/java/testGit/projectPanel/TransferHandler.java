package testGit.projectPanel;

import com.intellij.ui.treeStructure.SimpleTree;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

public class TransferHandler extends javax.swing.TransferHandler {
    private static final DataFlavor NODE_FLAVOR;

    static {
        try {
            NODE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + DefaultMutableTreeNode[].class.getName() + "\"");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to create custom DataFlavor", e);
        }
    }

    private final SimpleTree tree;
    private DefaultMutableTreeNode[] draggedNodes;

    public TransferHandler(SimpleTree tree) {
        this.tree = tree;
        tree.setDropMode(DropMode.ON);
        tree.setDragEnabled(true);
        tree.setTransferHandler(this);
        System.out.println("[INIT] TreeTransferHandler registered");
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return null;

        List<DefaultMutableTreeNode> nodeList = new ArrayList<>();
        for (TreePath path : paths) {
            nodeList.add((DefaultMutableTreeNode) path.getLastPathComponent());
        }

        draggedNodes = nodeList.toArray(new DefaultMutableTreeNode[0]);
        System.out.printf("[TRANSFERABLE] Dragging %d node(s)%n", draggedNodes.length);
        return new NodesTransferable(draggedNodes);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop() || !support.isDataFlavorSupported(NODE_FLAVOR)) return false;

        TreePath dropPath = ((SimpleTree.DropLocation) support.getDropLocation()).getPath();
        if (dropPath == null) return false;

        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) dropPath.getLastPathComponent();
        Object userObject = targetNode.getUserObject();

        if (!(userObject instanceof Directory targetInfo)) return false;

        DirectoryType targetType = targetInfo.getType();
        for (DefaultMutableTreeNode node : draggedNodes) {
            Object obj = node.getUserObject();
            if (!(obj instanceof Directory draggedInfo)) return false;

            // Block project moving or dropping into a feature
            if (draggedInfo.getType() == DirectoryType.P || targetType == DirectoryType.F)
                return false;

            // Block no-op
            if (node.getParent() == targetNode) return false;
        }

        return true;
    }

    @SneakyThrows
    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;

        Transferable t = support.getTransferable();
        DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) t.getTransferData(NODE_FLAVOR);

        TreePath dropPath = ((SimpleTree.DropLocation) support.getDropLocation()).getPath();
        DefaultMutableTreeNode newParent = (DefaultMutableTreeNode) dropPath.getLastPathComponent();

        Object parentObj = newParent.getUserObject();
        if (!(parentObj instanceof Directory newParentInfo)) return false;

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

        for (DefaultMutableTreeNode node : nodes) {
            Object userObj = node.getUserObject();
            if (!(userObj instanceof Directory movingInfo)) continue;

            // Remove from current parent
            model.removeNodeFromParent(node);

            // DB update

            // Insert under new parent
            model.insertNodeInto(node, newParent, newParent.getChildCount());

            System.out.printf("[MOVED] '%s' (id=%d) -> '%s' (id=%d)%n",
                    movingInfo.getName(), newParentInfo.getName());
        }

        return true;
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
