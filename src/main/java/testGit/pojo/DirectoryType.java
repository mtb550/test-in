package testGit.pojo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP("Test Project", AllIcons.Nodes.Project, TestProject.class),

    TCD("Test Cases Directory", AllIcons.Nodes.Bookmark, TestCasesDirectory.class),
    TRD("Test Runs Directory", AllIcons.Nodes.Bookmark, TestRunsDirectory.class),

    TSP("Test Set Package", AllIcons.Nodes.WebFolder, TestSetPackage.class),
    TRP("Test Run Package", AllIcons.Nodes.WebFolder, TestRunPackage.class),

    TS("Test Set", AllIcons.FileTypes.Text, TestSet.class),
    TR("Test Run", AllIcons.Nodes.Services, TestRun.class);


    private final String description;
    private final Icon icon;
    private final Class<? extends Directory> clazz;

    public static DirectoryType fromClass(Class<?> clazz) {
        for (DirectoryType type : values()) {
            if (type.getClazz() == clazz) {
                return type;
            }
        }
        return null;
    }
}