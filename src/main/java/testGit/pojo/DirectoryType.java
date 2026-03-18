package testGit.pojo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import testGit.pojo.dto.dirs.*;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP("Test Project", AllIcons.Nodes.Project, TestProjectDirectoryDto.class, ".tp"),

    TCD("Test Cases Directory", AllIcons.Nodes.Bookmark, TestCasesDirectoryDto.class, ".tcd"),
    TRD("Test Runs Directory", AllIcons.Nodes.Bookmark, TestRunsDirectoryDto.class, ".trd"),

    TSP("Test Set Package", AllIcons.Nodes.WebFolder, TestSetPackageDirectoryDto.class, ".tsp"),
    TRP("Test Run Package", AllIcons.Nodes.WebFolder, TestRunPackageDirectoryDto.class, ".trp"),

    TS("Test Set", AllIcons.FileTypes.Text, TestSetDirectoryDto.class, ".ts"),
    TR("Test Run", AllIcons.Nodes.Services, TestRunDirectoryDto.class, ".tr");


    private final String description;
    private final Icon icon;
    private final Class<? extends DirectoryDto> clazz;
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