package org.testin.pojo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.actions.CreateTestRun;
import org.testin.actions.CreateTestRunPackage;
import org.testin.actions.CreateTestSet;
import org.testin.actions.CreateTestSetPackage;
import org.testin.pojo.dto.dirs.*;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP(
            "Test Project",
            null,
            null,
            AllIcons.Nodes.Project,
            TestProjectDirectoryDto.class,
            ".tp",
            null
    ),

    TCD(
            "Test Cases Directory",
            "Test Cases",
            "testCases",
            AllIcons.Nodes.Bookmark,
            TestCasesMainDirectoryDto.class,
            ".tcd",
            null
    ),

    TRD(
            "Test Runs Directory",
            "Test Runs",
            "testRuns",
            AllIcons.Nodes.Bookmark,
            TestRunsMainDirectoryDto.class,
            ".trd",
            null
    ),

    TSP(
            "Test Set Package",
            null,
            null,
            AllIcons.Nodes.WebFolder,
            TestSetPackageDirectoryDto.class,
            ".tsp",
            new CreateTestSetPackage()
    ),

    TRP(
            "Test Run Package",
            null,
            null,
            AllIcons.Nodes.WebFolder,
            TestRunPackageDirectoryDto.class,
            ".trp",
            new CreateTestRunPackage()
    ),

    TS(
            "Test Set",
            null,
            null,
            AllIcons.FileTypes.Text,
            TestSetDirectoryDto.class,
            ".ts",
            new CreateTestSet()
    ),

    TR(
            "Test Run",
            null,
            null,
            AllIcons.Nodes.Services,
            TestRunDirectoryDto.class,
            ".tr",
            new CreateTestRun()
    ),

    IMPORT_TP(
            "Import Project (Git)",
            null,
            null,
            AllIcons.Vcs.Clone,
            TestProjectDirectoryDto.class,
            null,
            null
    );

    private final String description;
    private final String displayedName;
    private final String pathName;
    private final Icon icon;
    private final Class<? extends DirectoryDto> clazz;
    private final String marker;
    private final NodeCreator action;
}