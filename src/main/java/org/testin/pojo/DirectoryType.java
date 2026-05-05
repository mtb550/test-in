package org.testin.pojo;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.actions.*;
import org.testin.pojo.dto.dirs.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

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
    );

    private final String description;
    private final String displayedName;
    private final String pathName;
    private final Icon icon;
    private final Class<? extends DirectoryDto> clazz;
    private final String marker;
    private final NodeCreator creator;

    @FunctionalInterface
    public interface NodeCreator {
        void execute(final CreateTestNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath);
    }
}