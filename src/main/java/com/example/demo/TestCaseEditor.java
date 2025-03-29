package com.example.demo;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class TestCaseEditor {
    public static void open(String projectName, Feature feature) {
        Project ideProject = com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects()[0];
        VirtualFile virtualFile = new TestCaseVirtualFile(projectName, feature);
        FileEditorManager.getInstance(ideProject).openFile(virtualFile, true);

    }
}