package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.nio.file.Path;

public class AddModule extends AnAction {
    private final SimpleTree tree;

    public AddModule(final SimpleTree tree) {
        super("New Module", "Create a new module for this project", AllIcons.Nodes.Package);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            System.out.println("path is null!!");
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        // التحقق من أننا لا نضيف Suite داخل Feature
        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == DirectoryType.F) return;

        String name = Messages.showInputDialog("Enter suite name:", "Add Suite", null);
        if (name == null || name.isBlank()) return;
        name = name.replace("_", " ");

        // 1. تحديد مكان الإنشاء الفعلي على القرص
        Path parentPath = (treeItem.getType() == DirectoryType.P)
                ? treeItem.getFilePath().resolve("testCases")
                : treeItem.getFilePath();

        // 2. بناء بيانات الـ Suite الجديد
        Directory newSuite = new Directory()
                .setType(DirectoryType.S)
                .setName(name)
                .setActive(1);

        // استخدام الدالة المحسنة لاسم الملف
        String folderName = String.format("%s_%s_%d", newSuite.getType().name().toLowerCase(), newSuite.getName(), newSuite.getActive());
        Path fullPath = parentPath.resolve(folderName);

        newSuite.setFileName(folderName)
                .setFilePath(fullPath)
                .setFile(fullPath.toFile()); // ✅ مسار كامل Absolute Path

        // 3. التنفيذ داخل WriteAction (مطلوب لتعديلات الملفات في IntelliJ)
        WriteAction.run(() -> {
            try {
                // تحديث نظام الملفات للعثور على المجلد الأب
                VirtualFile parentVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(parentPath);

                if (parentVf != null && parentVf.isDirectory()) {
                    // إنشاء المجلد فعلياً
                    parentVf.createChildDirectory(this, folderName);

                    // تحديث الـ Tree Model
                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newSuite);

                    // إضافة النود وتنبيه المستمعين
                    model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());

                    // توسيع الشجرة واختيار العنصر الجديد
                    tree.makeVisible(new TreePath(newNode.getPath()));
                    tree.setSelectionPath(new TreePath(newNode.getPath()));
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Could not create directory: " + ex.getMessage(), "Error");
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean isFeature = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof Directory item &&
                item.getType() == DirectoryType.F);

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(!isFeature);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}