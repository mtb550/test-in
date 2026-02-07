package testGit.pojo;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import testGit.settings.AppSettingsState;

import javax.swing.*;
import java.io.File;
import java.nio.file.Paths;

public class Config {
    public static Icon porject = AllIcons.Nodes.Project;       // 📦 Project
    public static Icon folder = AllIcons.Nodes.Folder;       // 📁 Folder
    public static Icon clazz = AllIcons.Nodes.Class;     // 📄 Class
    public static Icon java = AllIcons.FileTypes.Java;     // ☕ Java file
    public static Icon json = AllIcons.FileTypes.Json;     // {} JSON
    public static Icon xml = AllIcons.FileTypes.Xml;     // <> XML
    public static Icon text = AllIcons.FileTypes.Text;     // 📝 Text
    public static Icon branch = AllIcons.Vcs.Branch;     // 🌿 Branch
    public static Icon add = AllIcons.General.Add;     // ➕ Add
    public static Icon remove = AllIcons.General.Remove;     // ➖ Remove
    @Setter
    @Getter
    private static String projectBasePath;
    @Setter
    @Getter
    private static Project project;
    private static String rootFolder;
    private static File rootFolderFile;
    private static File testCasesRootFolder;
    private static File testPlansRootFolder;

    public static void setRootFolder() {
        System.out.println("Config.setRootFolder()");

        String pathFromSettings = AppSettingsState.getInstance().rootFolderPath;
        System.out.println("pathFromSettings: " + pathFromSettings);
        System.out.println("projectBasePath: " + projectBasePath);

        if (pathFromSettings == null || pathFromSettings.isEmpty()) {
            rootFolder = projectBasePath + "/TestGit";
            rootFolderFile = Paths.get(rootFolder).toFile();
            System.out.println("rootFolder: " + rootFolder);
            System.out.println("rootFolder file: " + rootFolderFile.getAbsolutePath());
        } else {
            rootFolder = pathFromSettings;
            rootFolderFile = Paths.get(pathFromSettings).toFile();
            System.out.println("rootFolder: " + rootFolder);
            System.out.println("rootFolder file: " + rootFolderFile.getAbsolutePath());
        }
    }

    public static void setTestCasesRootFolder(Directory selectedProject) {
        System.out.println("Config.setTestCasesRootFolder()");

        testCasesRootFolder = Paths.get(rootFolder, selectedProject.getFileName(), "testCases").toFile();
        if (!testCasesRootFolder.exists()) {
            boolean created = testCasesRootFolder.mkdirs();
            if (created) {
                System.out.println("Test Cases Root folder created at: " + testCasesRootFolder.getAbsolutePath());
            }
        } else {
            System.out.println("Test Cases Root folder already exists at: " + testCasesRootFolder.getAbsolutePath());
        }
    }

    public static void setTestPlansRootFolder(Directory selectedProject) {
        System.out.println("Config.setTestPlansRootFolder()");

        testPlansRootFolder = Paths.get(rootFolder, selectedProject.getFileName(), "testPlans").toFile();


        if (!testPlansRootFolder.exists()) {
            boolean created = testPlansRootFolder.mkdirs();
            if (created) {
                System.out.println("Test Plans Root folder created at: " + testPlansRootFolder.getAbsolutePath());
            }
        } else {
            System.out.println("Test Plans Root folder already exists at: " + testPlansRootFolder.getAbsolutePath());
        }
    }

    public static File getTestCasesRootFolder(Directory selectedProject) {
        System.out.println("Config.getTestCasesRootFolder()");

        if (testCasesRootFolder == null) {
            setTestCasesRootFolder(selectedProject);
        }

        System.out.println("Config.getTestCasesRootFolder(): " + testCasesRootFolder);
        return testCasesRootFolder;
    }

    public static File getTestPlansRootFolder(Directory selectedProject) {
        System.out.println("Config.getTestPlansRootFolder()");

        if (testPlansRootFolder == null) {
            setTestPlansRootFolder(selectedProject);
        }

        System.out.println("Config.getTestPlansRootFolder(): " + testPlansRootFolder);
        return testPlansRootFolder;
    }

    public static File getRootFolderFile() {
        System.out.println("Config.getRootFolderFile()");

        if (rootFolderFile == null) {
            setRootFolder();
        }
        return rootFolderFile;
    }

    public static String getRootFolder() {
        System.out.println("Config.getRootFolder()");

        if (rootFolder == null) {
            setRootFolder();
        }
        return rootFolder;
    }

}