package org.testin.enums;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.mappers.dto.dirs.*;
import org.testin.nodeCreator.CreateTestRunPackage;
import org.testin.nodeCreator.CreateTestSet;
import org.testin.nodeCreator.CreateTestSetPackage;
import org.testin.nodeCreator.NodeCreator;
import org.testin.testRun.CreateTestRun;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP(
            "Test Project",
            null,
            AllIcons.Nodes.Project,
            TestProjectDirectoryDto.class,
            ".tp",
            null,
            (project, dir) -> GeneratorType.CREATE_TEST_SET_PACKAGE.getAction().execute(project, dir)
    ),

    TCD(
            "Test Cases Directory",
            "Test Cases",
            AllIcons.Nodes.Bookmark,
            TestCasesMainDirectoryDto.class,
            ".tcd",
            null,
            null
    ),

    TRD(
            "Test Runs Directory",
            "Test Runs",
            AllIcons.Nodes.Bookmark,
            TestRunsMainDirectoryDto.class,
            ".trd",
            null,
            null
    ),

    TSP(
            "Test Set Package",
            null,
            AllIcons.Nodes.WebFolder,
            TestSetPackageDirectoryDto.class,
            ".tsp",
            new CreateTestSetPackage(),
            (project, dir) -> GeneratorType.CREATE_TEST_SET_PACKAGE.getAction().execute(project, dir)
    ),

    TRP(
            "Test Run Package",
            null,
            AllIcons.Nodes.WebFolder,
            TestRunPackageDirectoryDto.class,
            ".trp",
            new CreateTestRunPackage(),
            null
    ),

    TS(
            "Test Set",
            null,
            AllIcons.FileTypes.Text,
            TestSetDirectoryDto.class,
            ".ts",
            new CreateTestSet(),
            (project, dir) -> GeneratorType.CREATE_TEST_SET.getAction().execute(project, dir)
    ),

    TR(
            "Test Run",
            null,
            AllIcons.Nodes.Services,
            TestRunDirectoryDto.class,
            ".tr",
            new CreateTestRun(),
            null
    ),

    IMPORT_TP(
            "Import Project (Git)",
            null,
            AllIcons.Vcs.Clone,
            TestProjectDirectoryDto.class,
            null,
            null,
            null
    );

    private final String description;
    private final String displayedName;
    private final Icon icon;
    private final Class<? extends DirectoryDto> clazz;
    private final String marker;
    private final NodeCreator action;
    private final GeneratorAction codeGenerator;
}