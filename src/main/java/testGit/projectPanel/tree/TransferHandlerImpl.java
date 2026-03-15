package testGit.projectPanel.tree;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestPackage;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
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
    private final Set<DefaultMutableTreeNode> cutNodes;
    private Integer lastAction;

    public TransferHandlerImpl(SimpleTree tree, Set<DefaultMutableTreeNode> cutNodes) {
        this.tree = tree;
        this.cutNodes = cutNodes;
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
            return dropPath != null && ((DefaultMutableTreeNode) dropPath.getLastPathComponent()).getUserObject() instanceof TestPackage;
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

            WriteAction.run(() -> {
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                for (DefaultMutableTreeNode node : nodes) {
                    if (action == MOVE) {
                        if (node.equals(targetNode) || node.isNodeDescendant(targetNode)) continue;
                        model.removeNodeFromParent(node);
                        persistMove((TestPackage) node.getUserObject(), (TestPackage) targetNode.getUserObject());
                        model.insertNodeInto(node, targetNode, targetNode.getChildCount());
                    } else {
                        DefaultMutableTreeNode clone = cloneNode(node);
                        persistCopy((TestPackage) node.getUserObject(), (TestPackage) targetNode.getUserObject(), (TestPackage) clone.getUserObject());
                        model.insertNodeInto(clone, targetNode, targetNode.getChildCount());
                    }
                }
                resetLastAction();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private DefaultMutableTreeNode cloneNode(DefaultMutableTreeNode node) {
        TestPackage dir = (TestPackage) node.getUserObject();
        TestPackage newDir = new TestPackage()
                .setName(dir.getName())
                .setFileName(dir.getFileName())
                .setFile(dir.getFile())
                .setFilePath(dir.getFilePath())
                .setType(dir.getType())
                .setIcon(dir.getIcon());
        return new DefaultMutableTreeNode(newDir);
    }

    private void persistMove(TestPackage source, TestPackage target) {
        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(source.getFile());
        VirtualFile targetDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(target.getFile());

        try {
            if (vFile != null && targetDir != null) {
                vFile.move(this, targetDir);
                Path newPath = target.getFilePath().resolve(source.getFileName());
                source.setFilePath(newPath).setFile(newPath.toFile());
            }
        } catch (IOException ignored) {
        }
    }

    private void persistCopy(TestPackage source, TestPackage target, TestPackage cloned) {
        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(source.getFile());
        VirtualFile targetDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(target.getFile());

        try {
            if (vFile != null && targetDir != null) {
                vFile.copy(this, targetDir, vFile.getName());
                Path newPath = target.getFilePath().resolve(source.getFileName());
                cloned.setFilePath(newPath).setFile(newPath.toFile());
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        resetLastAction();
    }

    public void resetLastAction() {
        this.lastAction = null;
        cutNodes.clear();
        tree.repaint();
    }

    @Override
    public void exportToClipboard(JComponent comp, java.awt.datatransfer.Clipboard clip, int action) throws IllegalStateException {
        super.exportToClipboard(comp, clip, action);
        this.lastAction = action;

        cutNodes.clear();
        if (action == MOVE) {
            TreePath[] paths = tree.getSelectionPaths();
            if (paths != null) {
                for (TreePath path : paths) {
                    cutNodes.add((DefaultMutableTreeNode) path.getLastPathComponent());
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