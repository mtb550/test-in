package org.testin.pojo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.pojo.dto.dirs.*;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP("Test Project", null, null, AllIcons.Nodes.Project, TestProjectDirectoryDto.class, ".tp"),

    TCD("Test Cases Directory", "Test Cases", "testCases", AllIcons.Nodes.Bookmark, TestCasesDirectoryDto.class, ".tcd"),
    TRD("Test Runs Directory", "Test Runs", "testRuns", AllIcons.Nodes.Bookmark, TestRunsDirectoryDto.class, ".trd"),

    TSP("Test Set Package", null, null, AllIcons.Nodes.WebFolder, TestSetPackageDirectoryDto.class, ".tsp"),
    TRP("Test Run Package", null, null, AllIcons.Nodes.WebFolder, TestRunPackageDirectoryDto.class, ".trp"),

    TS("Test Set", null, null, AllIcons.FileTypes.Text, TestSetDirectoryDto.class, ".ts"),
    TR("Test Run", null, null, AllIcons.Nodes.Services, TestRunDirectoryDto.class, ".tr");


    private final String description;
    private final String displayedName;
    private final String pathName;
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