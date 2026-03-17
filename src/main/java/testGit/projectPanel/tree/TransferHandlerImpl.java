package testGit.projectPanel.tree;

import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
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

public class TransferHandlerImpl extends TransferHandler {
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

    public TransferHandlerImpl(SimpleTree tree, Set<DefaultMutableTreeNode> selectedNodes) {
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

            if (targetNode == null) return false;

            int action = support.isDrop() ? support.getDropAction() : (this.lastAction != null ? this.lastAction : COPY);

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            for (DefaultMutableTreeNode node : nodes) {
                if (action == MOVE) {

                    if (node.equals(targetNode) || node.isNodeDescendant(targetNode)) continue;
                    model.removeNodeFromParent(node);
                    persistMove((Directory) node.getUserObject(), (Directory) targetNode.getUserObject());
                    model.insertNodeInto(node, targetNode, targetNode.getChildCount());

                } else {
                    DefaultMutableTreeNode clone = cloneNode(node);
                    persistCopy((Directory) node.getUserObject(), (Directory) targetNode.getUserObject(), (Directory) clone.getUserObject());
                    model.insertNodeInto(clone, targetNode, targetNode.getChildCount());
                }
            }

            resetLastAction();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private DefaultMutableTreeNode cloneNode(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();

        if (userObject instanceof Directory dir) {
            try {
                Directory newDir = dir.getClass().getDeclaredConstructor().newInstance();

                newDir.setName(dir.getName());
                newDir.setPath(dir.getPath());

                return new DefaultMutableTreeNode(newDir);

            } catch (Exception e) {
                System.err.println("Failed to clone node: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        return new DefaultMutableTreeNode(userObject);
    }

    private void persistMove(Directory source, Directory target) {
        TreeUtilImpl.executeVfsAction(source.getPath(), target.getPath(), "Move Failed", (sourceVf, targetVf) -> {

            sourceVf.move(this, targetVf);

            String actualFileName = source.getPath().getFileName().toString();

            Path newPath = target.getPath().resolve(actualFileName);
            source.setPath(newPath);

            System.out.println("Moved successfully to: " + newPath);
        });
    }

    private void persistCopy(Directory source, Directory target, Directory cloned) {
        TreeUtilImpl.executeVfsAction(source.getPath(), target.getPath(), "Copy Failed", (sourceVf, targetVf) -> {
            sourceVf.copy(this, targetVf, sourceVf.getName());
            Path newPath = target.getPath().resolve(source.getName());
            cloned.setPath(newPath);
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