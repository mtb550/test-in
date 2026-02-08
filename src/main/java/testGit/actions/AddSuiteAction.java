package testGit.actions;

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

public class AddSuiteAction extends AnAction {
    private final SimpleTree tree;

    public AddSuiteAction(final SimpleTree tree) {
        super("➕ New Suite");
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
}