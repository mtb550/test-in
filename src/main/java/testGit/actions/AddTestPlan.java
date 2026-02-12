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

public class AddTestPlan extends AnAction {
    private final SimpleTree tree;

    public AddTestPlan(final SimpleTree tree) {
        super("New Test Plan", "Create a new execution run for this plan", AllIcons.Actions.ListFiles);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            System.out.println("path is null !!");
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        // التحقق من أننا لا نضيف Suite داخل Feature
        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == DirectoryType.TR) return;

        String name = Messages.showInputDialog("Enter test plan name:", "Add Test Plan", AllIcons.RunConfigurations.TestState.Run);
        if (name == null || name.isBlank()) return;
        name = name.replace("_", " ");

        // 1. تحديد مكان الإنشاء الفعلي على القرص
        Path parentPath = (treeItem.getType() == DirectoryType.P)
                ? treeItem.getFilePath().resolve("testPlans")
                : treeItem.getFilePath();

        // 2. بناء بيانات الـ Suite الجديد
        Directory newTestPlan = new Directory()
                .setType(DirectoryType.TP)
                .setName(name)
                .setActive(1);

        // استخدام الدالة المحسنة لاسم الملف
        String folderName = String.format("%s_%s_%d", newTestPlan.getType().name().toLowerCase(), newTestPlan.getName(), newTestPlan.getActive());
        Path fullPath = parentPath.resolve(folderName);

        newTestPlan.setFileName(folderName)
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
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newTestPlan);

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

        boolean isTestPlan = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof Directory item &&
                (item.getType() == DirectoryType.TP || item.getType() == DirectoryType.TR));

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(!isTestPlan);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
