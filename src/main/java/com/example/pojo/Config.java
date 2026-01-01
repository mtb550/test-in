package com.example.pojo;

import com.example.settings.AppSettingsState;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

public class Config {
    @Setter
    @Getter
    private static String projectBasePath;
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