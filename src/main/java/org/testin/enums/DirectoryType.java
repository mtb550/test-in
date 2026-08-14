package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.GeneratorAction;
import org.testin.codegen.GeneratorType;
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
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestProject(dir.getPath(), () -> {
                GeneratorType.REMOVE_TEST_PROJECT.getAction().execute(p, dir);
                onRemoved.run();
            }),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
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
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSetPackage(dir.getPath(), () -> {
                GeneratorType.REMOVE_TEST_SET_PACKAGE.getAction().execute(p, dir);
                onRemoved.run();
            }),
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
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRunPackage(dir.getPath(), onRemoved),
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
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSet(dir.getPath(), () -> {
                GeneratorType.REMOVE_TEST_SET.getAction().execute(p, dir);
                onRemoved.run();
            }),
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
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRun(dir.getPath(), onRemoved),
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

    private static final @NotNull Map<Class<?>, DirectoryType> BY_CLASS;

    static {
        final Map<Class<?>, DirectoryType> map = new HashMap<>();
        for (final DirectoryType type : values())
            map.putIfAbsent(type.clazz, type);

        BY_CLASS = Map.copyOf(map);
    }

    private final @NotNull String description;

    /**
     * Null when the node shows its own name rather than a fixed label.
     */
    private final @Nullable String displayedName;
    private final @NotNull Icon icon;
    private final @NotNull Class<? extends DirectoryDto> clazz;

    /**
     * Null for IMPORT_TP: a clone target has no marker file of its own.
     */
    private final @Nullable String marker;

    // Null where the type does not support that operation: the fixed root
    // containers cannot be created, some types generate no Java code, and the
    // roots cannot be removed. Call sites check before invoking.
    private final @Nullable Function<Project, NodeCreator> action;
    private final @Nullable GeneratorAction codeGenerator;
    private final @Nullable RemoveHandler removeHandler;
    private final @NotNull SimpleTextAttributes attributes;

    public static @Nullable DirectoryType from(final @NotNull DirectoryDto dir) {
        return BY_CLASS.get(dir.getClass());
    }
}
