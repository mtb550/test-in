package testGit.pojo;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import testGit.settings.AppSettingsState;

import javax.swing.*;
import java.io.File;

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
    private static File rootFolder;

    public static void setRootFolder() {
        System.out.println("Config.setRootFolder()");

        String pathFromSettings = AppSettingsState.getInstance().rootFolderPath;
        if (pathFromSettings == null || pathFromSettings.isEmpty()) {
            pathFromSettings = projectBasePath + "/TestGit";
        }

        rootFolder = new File(pathFromSettings);

        if (!rootFolder.exists()) {
            boolean created = rootFolder.mkdirs();
            if (created) {
                System.out.println("Root folder created at: " + rootFolder.getAbsolutePath());
            }
        } else {
            System.out.println("Root folder already exists at: " + rootFolder.getAbsolutePath());
        }
    }

    public static File getRootFolder() {
        System.out.println("Config.getRootFolder()");

        if (rootFolder == null) {
            setRootFolder();
        }
        return rootFolder;
    }

}