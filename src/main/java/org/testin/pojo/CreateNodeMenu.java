package org.testin.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.pojo.dto.dirs.*;

import java.util.List;

@Getter
@AllArgsConstructor
public enum CreateNodeMenu {

    TEST_PROJECT(
            "Create Project",
            List.of(DirectoryType.TP),
            TestProjectDirectoryDto.class,
            DirectoryType.TP,
            "set name.."
    ),

    TEST_CASES_MAIN_DIR(
            "Create Test Node",
            List.of(DirectoryType.TS, DirectoryType.TSP),
            TestCasesMainDirectoryDto.class,
            DirectoryType.TCD,
            "set name.."
    ),

    TEST_RUNS_MAIN_DIR(
            "Create Run Node",
            List.of(DirectoryType.TR, DirectoryType.TRP),
            TestRunsMainDirectoryDto.class,
            DirectoryType.TRD,
            "set name.."
    ),

    TEST_SET_PACKAGE(
            "Create Test Node",
            List.of(DirectoryType.TS, DirectoryType.TSP),
            TestSetPackageDirectoryDto.class,
            DirectoryType.TSP,
            "set name.."
    ),

    TEST_RUN_PACKAGE(
            "Create Run Node",
            List.of(DirectoryType.TR, DirectoryType.TRP),
            TestRunPackageDirectoryDto.class,
            DirectoryType.TRP,
            "set name.."
    ),

    TEST_SET(
            "Create Test Set",
            List.of(),
            TestSetDirectoryDto.class,
            DirectoryType.TS,
            "set name.."
    ),

    TEST_RUN(
            "Create Test Run",
            List.of(),
            TestRunDirectoryDto.class,
            DirectoryType.TR,
            "set name.."
    );

    private final String title;
    private final List<DirectoryType> availableOptions;
    private final Class<?> targetDtoClass;
    private final DirectoryType targetParentType;
    private final String placeholder;

}