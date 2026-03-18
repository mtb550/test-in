package testGit.pojo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import testGit.pojo.tree.dirs.*;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP("Test Project", AllIcons.Nodes.Project, TestProjectDirectory.class, ".tp"),

    TCD("Test Cases Directory", AllIcons.Nodes.Bookmark, TestCasesDirectory.class, ".tcd"),
    TRD("Test Runs Directory", AllIcons.Nodes.Bookmark, TestRunsDirectory.class, ".trd"),

    TSP("Test Set Package", AllIcons.Nodes.WebFolder, TestSetPackageDirectory.class, ".tsp"),
    TRP("Test Run Package", AllIcons.Nodes.WebFolder, TestRunPackageDirectory.class, ".trp"),

    TS("Test Set", AllIcons.FileTypes.Text, TestSetDirectory.class, ".ts"),
    TR("Test Run", AllIcons.Nodes.Services, TestRunDirectory.class, ".tr");


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