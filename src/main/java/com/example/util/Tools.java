package com.example.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;

public class Tools {
    public static void printTestSourceRoots(Project project) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        for (VirtualFile root : ProjectRootManager.getInstance(project).getContentSourceRoots()) {
            if (fileIndex.isInTestSourceContent(root)) {
                System.out.println("🧪 Test Source Root: " + root.getPath());
            }
        }
    }
}
