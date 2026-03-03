package testGit.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import testGit.settings.AppSettingsState;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Config {
    public static Icon folder = AllIcons.Nodes.Folder;
    public static Icon clazz = AllIcons.Nodes.Class;
    public static Icon java = AllIcons.FileTypes.Java;
    public static Icon json = AllIcons.FileTypes.Json;
    public static Icon xml = AllIcons.FileTypes.Xml;
    public static Icon text = AllIcons.FileTypes.Text;
    public static Icon branch = AllIcons.Vcs.Branch;
    public static Icon add = AllIcons.General.Add;
    public static Icon remove = AllIcons.General.Remove;

    @Setter
    @Getter
    private static String projectBasePath;
    @Setter
    @Getter
    private static Project project;
    private static String rootFolder;
    @Getter
    private static File rootFolderFile;
    private static File testCasesRootFolder;
    private static File testRunsRootFolder;

    public static Icon getProjectIcon() {
        return AllIcons.Nodes.Project;
    }

    public static void setRootFolder() {
        String pathFromSettings = AppSettingsState.getInstance().rootFolderPath;
        String targetPath = (pathFromSettings == null || pathFromSettings.isEmpty())
                ? projectBasePath + "/TestGit"
                : pathFromSettings;

        VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(targetPath);
        if (vFile != null) {
            rootFolder = vFile.getPath();
            rootFolderFile = new File(vFile.getPath());
        } else {
            rootFolder = targetPath;
            rootFolderFile = new File(targetPath);
        }
    }

    public static void setTestCasesRootFolder(Directory selectedProject) {
        testCasesRootFolder = createDirectorySafely(selectedProject.getFileName(), "testCases");
    }

    public static void setTestRunsRootFolder(Directory selectedProject) {
        testRunsRootFolder = createDirectorySafely(selectedProject.getFileName(), "testRuns");
    }

    private static File createDirectorySafely(String projectName, String subFolder) {
        if (rootFolder == null) setRootFolder();

        try {
            VirtualFile base = LocalFileSystem.getInstance().refreshAndFindFileByPath(rootFolder);
            if (base == null) {
                File fallback = new File(rootFolder, projectName + "/" + subFolder);
                fallback.mkdirs();
                return fallback;
            }

            VirtualFile result = VfsUtil.createDirectoryIfMissing(base, projectName + "/" + subFolder);
            return result != null ? new File(result.getPath()) : null;

        } catch (IOException e) {
            System.err.println("TestGit: Failed to create directories: " + e.getMessage());
            return new File(rootFolder, projectName + "/" + subFolder);
        }
    }

    public static File getTestCasesRootFolder(Directory selectedProject) {
        if (testCasesRootFolder == null) setTestCasesRootFolder(selectedProject);
        return testCasesRootFolder;
    }

    public static File getTestRunsRootFolder(Directory selectedProject) {
        if (testRunsRootFolder == null) setTestRunsRootFolder(selectedProject);
        return testRunsRootFolder;
    }

    public static ObjectMapper getMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}