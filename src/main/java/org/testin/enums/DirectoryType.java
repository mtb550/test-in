package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
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
import java.util.function.Function;

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
            (p, dir) -> GeneratorType.CREATE_TEST_SET_PACKAGE.getAction().execute(p, dir)
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
            CreateTestSetPackage::new,
            (p, dir) -> GeneratorType.CREATE_TEST_SET_PACKAGE.getAction().execute(p, dir)
    ),

    TRP(
            "Test Run Package",
            null,
            AllIcons.Nodes.WebFolder,
            TestRunPackageDirectoryDto.class,
            ".trp",
            CreateTestRunPackage::new,
            null
    ),

    TS(
            "Test Set",
            null,
            AllIcons.FileTypes.Text,
            TestSetDirectoryDto.class,
            ".ts",
            CreateTestSet::new,
            (p, dir) -> GeneratorType.CREATE_TEST_SET.getAction().execute(p, dir)
    ),

    TR(
            "Test Run",
            null,
            AllIcons.Nodes.Services,
            TestRunDirectoryDto.class,
            ".tr",
            CreateTestRun::new,
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
    private final Function<Project, NodeCreator> action;
    private final GeneratorAction codeGenerator;
}