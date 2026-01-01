package com.example.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;

public class Tools {
    public static void printTestSourceRoots(Project project) {
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

    public static void refreshPath(Path path) {
        if (path == null) return;

        // استخدام ApplicationManager لضمان تشغيل الكود في سياق آمن
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteAction.run(() -> {
                VfsUtil.markDirtyAndRefresh(false, true, true, path.toFile());
            });
        }, ModalityState.defaultModalityState());
    }
}
