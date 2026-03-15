package testGit.projectPanel.tree;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestPackage;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
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
        System.out.println("TransferHandlerImpl.TransferHandlerImpl()");
        this.tree = tree;
        this.cutNodes = cutNodes;
    }


    @Override
    public int getSourceActions(JComponent c) {
        System.out.println("TransferHandlerImpl.getSourceActions " + c.getName());
        return COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        System.out.println("TransferHandlerImpl.createTransferable. path: " + Arrays.toString(tree.getSelectionPaths()));

        TreePath[] paths = tree.getSelectionPaths();
        Arrays.stream(Objects.requireNonNull(tree.getSelectionPaths())).forEach(treePath -> System.out.println(treePath.getLastPathComponent()));

        if (paths == null) return null;

        DefaultMutableTreeNode[] nodes = Arrays.stream(paths)
                .map(path -> (DefaultMutableTreeNode) path.getLastPathComponent())
                .toArray(DefaultMutableTreeNode[]::new);

        return new NodesTransferable(nodes);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        System.out.println("TransferHandlerImpl.canImport()");
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) {
            System.out.println("TransferHandlerImpl.canImport(). not support");
            return false;
        }

        if (support.isDrop()) {
            System.out.println("TransferHandlerImpl.canImport(). drop support");

            TreePath dropPath = ((SimpleTree.DropLocation) support.getDropLocation()).getPath();
            if (dropPath == null) return false;
            DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) dropPath.getLastPathComponent();
            return isValidTarget(targetNode);
        }

        return tree.getSelectionPath() != null;
    }

    private boolean isValidTarget(DefaultMutableTreeNode targetNode) {
        System.out.println("TransferHandlerImpl.isValidTarget()");
        return targetNode.getUserObject() instanceof TestPackage targetDir;
    }

    @Override
    public boolean importData(TransferSupport support) {
        System.out.println("TransferHandlerImpl.importData()");

        if (!canImport(support)) return false;

        try {
            DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) support.getTransferable().getTransferData(NODE_FLAVOR);
            DefaultMutableTreeNode targetNode;

            if (support.isDrop()) {
                targetNode = (DefaultMutableTreeNode) ((SimpleTree.DropLocation) support.getDropLocation()).getPath().getLastPathComponent();
            } else {
                targetNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            }

            if (targetNode == null) return false;

            if (targetNode.getUserObject() instanceof TestPackage targetDir) {
                if (targetDir.getType() == DirectoryType.PR) {
                    Path newPath = targetDir.getFilePath().resolve("testCases");
                    targetDir.setFilePath(newPath).setFile(newPath.toFile());

                    targetNode.setUserObject(targetDir);
                }
                System.out.println("import data, target: " + targetDir.getFilePath());

            }

            if (this.lastAction == null) return false;
            int action = support.isDrop() ? support.getDropAction() : this.lastAction;

            WriteAction.run(() -> {
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                for (DefaultMutableTreeNode node : nodes) {
                    if (action == MOVE) {
                        System.out.println("action is move");
                        model.removeNodeFromParent(node);
                        persistMove((TestPackage) node.getUserObject(), (TestPackage) targetNode.getUserObject());
                        model.insertNodeInto(node, targetNode, targetNode.getChildCount());
                    } else {
                        System.out.println("action is copy");
                        DefaultMutableTreeNode clone = cloneNode(node);
                        persistCopy((TestPackage) node.getUserObject(), (TestPackage) targetNode.getUserObject());
                        model.insertNodeInto(clone, targetNode, targetNode.getChildCount());
                    }
                }

                cutNodes.clear();
                tree.repaint();
            });

            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    private DefaultMutableTreeNode cloneNode(DefaultMutableTreeNode node) {
        TestPackage dir = (TestPackage) node.getUserObject();
        TestPackage newDir = new TestPackage()
                .setType(dir.getType())
                .setName(dir.getName())
                .setFile(dir.getFile());

        return new DefaultMutableTreeNode(newDir);
    }

    private void persistMove(TestPackage source, TestPackage target) {
        System.out.println("Persisting MOVE to disk: " + source.getFileName());

        VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(source.getFile());
        VirtualFile targetDir = LocalFileSystem.getInstance().findFileByIoFile(target.getFile());

        System.out.println("source:: " + source.getFilePath());
        System.out.println("target:: " + target.getFilePath());
        try {
            if (vFile != null && targetDir != null)
                vFile.move(this, targetDir);
        } catch (java.io.IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private void persistCopy(TestPackage source, TestPackage target) {
        System.out.println("Persisting COPY to disk: " + source.getFileName());
        VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(source.getFile());
        VirtualFile targetDir = LocalFileSystem.getInstance().findFileByIoFile(target.getFile());
        try {
            if (vFile != null && targetDir != null) vFile.copy(this, targetDir, vFile.getName());
        } catch (java.io.IOException e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        System.out.println("TransferHandlerImpl.exportDone()");
        this.lastAction = action;

        if (action == MOVE) {
            System.out.println("MMMMMMove action");
            try {
                DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) data.getTransferData(NODE_FLAVOR);
                cutNodes.clear();
                cutNodes.addAll(Arrays.asList(nodes));

                tree.repaint();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            System.out.println("Nodes cut successfully, cleaning up source...");
        }
    }

    public void resetLastAction() {
        this.lastAction = null;
    }

    private record NodesTransferable(DefaultMutableTreeNode[] nodes) implements Transferable {
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            System.out.println("TransferHandlerImpl.getTransferDataFlavors()");
            return new DataFlavor[]{NODE_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            System.out.println("TransferHandlerImpl.isDataFlavorSupported()");
            return NODE_FLAVOR.equals(flavor);
        }

        @Override
        public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            System.out.println("TransferHandlerImpl.getTransferData()");
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return nodes;
        }
    }
}