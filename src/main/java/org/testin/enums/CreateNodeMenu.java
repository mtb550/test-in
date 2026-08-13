package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorType;
import org.testin.mappers.dto.dirs.*;

import java.util.List;

@Getter
@AllArgsConstructor
public enum CreateNodeMenu {

    TEST_PROJECT(
            "Create Project",
            List.of(DirectoryType.TP, DirectoryType.IMPORT_TP),
            TestProjectDirectoryDto.class,
            DirectoryType.TP,
            "set name or paste url..",
            GeneratorType.CREATE_TEST_PROJECT
    ),

    TEST_CASES_MAIN_DIR(
            "Create Test Node",
            List.of(DirectoryType.TS, DirectoryType.TSP),
            TestCasesMainDirectoryDto.class,
            DirectoryType.TCD,
            "set name..",
            null
    ),

    TEST_RUNS_MAIN_DIR(
            "Create Run Node",
            List.of(DirectoryType.TR, DirectoryType.TRP),
            TestRunsMainDirectoryDto.class,
            DirectoryType.TRD,
            "set name..",
            null
    ),

    TEST_SET_PACKAGE(
            "Create Test Node",
            List.of(DirectoryType.TS, DirectoryType.TSP),
            TestSetPackageDirectoryDto.class,
            DirectoryType.TSP,
            "set name..",
            GeneratorType.CREATE_TEST_SET_PACKAGE
    ),

    TEST_RUN_PACKAGE(
            "Create Run Node",
            List.of(DirectoryType.TR, DirectoryType.TRP),
            TestRunPackageDirectoryDto.class,
            DirectoryType.TRP,
            "set name..",
            null
    ),

    TEST_SET(
            "Create Test Set",
            List.of(),
            TestSetDirectoryDto.class,
            DirectoryType.TS,
            "set name..",
            GeneratorType.CREATE_TEST_SET
    ),

    TEST_RUN(
            "Create Test Run",
            List.of(),
            TestRunDirectoryDto.class,
            DirectoryType.TR,
            "set name..",
            null
    );

    private final @NotNull String title;
    private final @NotNull List<DirectoryType> availableOptions;
    private final @NotNull Class<?> targetDtoClass;
    private final @NotNull DirectoryType targetParentType;
    private final @NotNull String placeholder;
    private final @NotNull GeneratorType generatorType;

}
