package testGit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.nio.file.Path;

public class Tools {
    public static void printTestSourceRoots(Project project) {
        System.out.println("printTestSourceRoots.printTestSourceRoots()");

        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        for (VirtualFile root : ProjectRootManager.getInstance(project).getContentSourceRoots()) {
            if (fileIndex.isInTestSourceContent(root)) {
                System.out.println("🧪 Test Source Root: " + root.getPath());
            }
        }
    }

    // ✅ إجبار IntelliJ على قراءة محتويات المجلد من القرص الصلب قبل بناء الشجرة
    /*public static void refreshPath(Path path) {
        VfsUtil.markDirtyAndRefresh(false, true, true, path.toFile());
    }*/

    public static void refreshPath(final Path path) {
        System.out.println("Tools.refreshPath()");

        if (path == null) return;

        // تحويل Path إلى VirtualFile لضمان كفاءة التعامل مع نظام ملفات IntelliJ
        VirtualFile virtualFile = VfsUtil.findFileByIoFile(path.toFile(), true);

        if (virtualFile != null) {
            // نستخدم refresh غير متزامن (async: true)
            // لضمان تحديث الشجرة فوراً وبدون تجميد الواجهة
            virtualFile.refresh(true, true);
        } else {
            // في حال كان الملف جديداً تماماً ولم يره IntelliJ بعد
            // نطلب من نظام الملفات اكتشافه من المجلد الأب
            File parentFile = path.toFile().getParentFile();
            if (parentFile != null) {
                VfsUtil.markDirtyAndRefresh(true, true, true, parentFile);
            }
        }
    }

    /**
     * Generate unique project ID
     * In production, this should come from database
     */
    public static int generateUniqueId() {
        return (int) (System.currentTimeMillis() % 100000);
    }

    /**
     * Refreshes the Virtual File System (VFS) for a specific path.
     * Use this after deleting or creating files via java.io.File.
     */
    public static void refreshFileSystem(final File ioFile) {
        System.out.println("Tools.refreshFileSystem()");
        // Use the asynchronous version to prevent UI lag/flicker
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile);

    }

}
