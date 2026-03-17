package testGit.pojo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP("Test Project", AllIcons.Nodes.Project, TestProject.class, ".tp"),

    TCD("Test Cases Directory", AllIcons.Nodes.Bookmark, TestCasesDirectory.class, ".tcd"),
    TRD("Test Runs Directory", AllIcons.Nodes.Bookmark, TestRunsDirectory.class, ".trd"),

    TSP("Test Set Package", AllIcons.Nodes.WebFolder, TestSetPackage.class, ".tsp"),
    TRP("Test Run Package", AllIcons.Nodes.WebFolder, TestRunPackage.class, ".trp"),

    TS("Test Set", AllIcons.FileTypes.Text, TestSet.class, ".ts"),
    TR("Test Run", AllIcons.Nodes.Services, TestRun.class, ".tr");


    private final String description;
    private final Icon icon;
    private final Class<? extends Directory> clazz;
    private final String marker;

    public static DirectoryType fromClass(Class<?> clazz) {
        for (DirectoryType type : values()) {
            if (type.getClazz() == clazz) {
                return type;
            }
        }
        return null;
    }
}