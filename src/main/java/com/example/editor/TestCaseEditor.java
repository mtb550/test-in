package com.example.editor;

import com.example.demo.TestCaseVirtualFile;
import com.example.pojo.Feature;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;

public class TestCaseEditor {
    public static void open(String projectName, Feature feature) {
        Project ideProject = ProjectManager.getInstance().getOpenProjects()[0];
        FileEditorManager editorManager = FileEditorManager.getInstance(ideProject);

        for (VirtualFile openFile : editorManager.getOpenFiles()) {
            if (openFile instanceof TestCaseVirtualFile existing &&
                    existing.getProjectName().equals(projectName) &&
                    existing.getFeature().getName().equals(feature.getName())) {
                editorManager.openFile(existing, true); // focus existing
                return;
            }
        }

        // Else open new tab
        VirtualFile virtualFile = new TestCaseVirtualFile(projectName, feature);
        editorManager.openFile(virtualFile, true);
    }
}
