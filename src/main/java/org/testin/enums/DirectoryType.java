package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.dirs.*;
import org.testin.nodeCreator.*;
import org.testin.services.Services;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
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
            (p, dir) -> GeneratorType.CREATE_TEST_SET_PACKAGE.getAction().execute(p, dir),
            (p, dir) -> {
                Services.getInstance(p, ProjectIndexer.class).removeTestProject(dir.getPath());
                GeneratorType.REMOVE_TEST_PROJECT.getAction().execute(p, dir);
            },
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    ),

    TCD(
            "Test Cases Directory",
            "Test Cases",
            AllIcons.Nodes.Bookmark,
            TestCasesMainDirectoryDto.class,
            ".tcd",
            null,
            null,
            null,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
    ),

    TRD(
            "Test Runs Directory",
            "Test Runs",
            AllIcons.Nodes.Bookmark,
            TestRunsMainDirectoryDto.class,
            ".trd",
            null,
            null,
            null,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
    ),

    TSP(
            "Test Set Package",
            null,
            AllIcons.Nodes.WebFolder,
            TestSetPackageDirectoryDto.class,
            ".tsp",
            CreateTestSetPackage::new,
            (p, dir) -> GeneratorType.CREATE_TEST_SET_PACKAGE.getAction().execute(p, dir),
            (p, dir) -> {
                Services.getInstance(p, ProjectIndexer.class).removeTestSetPackage(dir.getPath());
                GeneratorType.REMOVE_TEST_SET_PACKAGE.getAction().execute(p, dir);
            },
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    ),

    TRP(
            "Test Run Package",
            null,
            AllIcons.Nodes.WebFolder,
            TestRunPackageDirectoryDto.class,
            ".trp",
            CreateTestRunPackage::new,
            null,
            (p, dir) -> Services.getInstance(p, ProjectIndexer.class).removeTestRunPackage(dir.getPath()),
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    ),

    TS(
            "Test Set",
            null,
            AllIcons.FileTypes.Text,
            TestSetDirectoryDto.class,
            ".ts",
            CreateTestSet::new,
            (p, dir) -> GeneratorType.CREATE_TEST_SET.getAction().execute(p, dir),
            (p, dir) -> {
                Services.getInstance(p, ProjectIndexer.class).removeTestSet(dir.getPath());
                GeneratorType.REMOVE_TEST_SET.getAction().execute(p, dir);
            },
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    ),

    TR(
            "Test Run",
            null,
            AllIcons.Nodes.Services,
            TestRunDirectoryDto.class,
            ".tr",
            CreateTestRun::new,
            null,
            (p, dir) -> Services.getInstance(p, ProjectIndexer.class).removeTestRun(dir.getPath()),
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    ),

    IMPORT_TP(
            "Import Project (Git)",
            null,
            AllIcons.Vcs.Clone,
            TestProjectDirectoryDto.class,
            null,
            null,
            null,
            null,
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    );

    private final String description;
    private final String displayedName;
    private final Icon icon;
    private final Class<? extends DirectoryDto> clazz;
    private final String marker;
    private final Function<Project, NodeCreator> action;
    private final GeneratorAction codeGenerator;
    private final RemoveHandler removeHandler;
    private final SimpleTextAttributes attributes;
    private static final Map<Class<?>, DirectoryType> BY_CLASS;

    static {
        final Map<Class<?>, DirectoryType> map = new HashMap<>();
        for (final DirectoryType type : values())
            map.putIfAbsent(type.clazz, type);

        BY_CLASS = Map.copyOf(map);
    }

    public static @Nullable DirectoryType from(final DirectoryDto dir) {
        return BY_CLASS.get(dir.getClass());
    }
}
