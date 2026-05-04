package org.testin.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.pojo.dto.dirs.*;

@Getter
@AllArgsConstructor
public enum CreateNodeMenu {

    TEST_PROJECT(
            "Create Project",
            new DirectoryType[]{},
            TestProjectDirectoryDto.class,
            DirectoryType.TP
    ),

    TEST_CASES_DIR(
            "Create Test Node",
            new DirectoryType[]{DirectoryType.TS, DirectoryType.TSP},
            TestCasesDirectoryDto.class,
            DirectoryType.TCD
    ),

    TEST_RUNS_DIR(
            "Create Run Node",
            new DirectoryType[]{DirectoryType.TR, DirectoryType.TRP},
            TestRunsDirectoryDto.class,
            DirectoryType.TRD
    ),

    TEST_SET_PACKAGE(
            "Create Test Node",
            new DirectoryType[]{DirectoryType.TS, DirectoryType.TSP},
            TestSetPackageDirectoryDto.class,
            DirectoryType.TSP
    ),

    TEST_RUN_PACKAGE(
            "Create Run Node",
            new DirectoryType[]{DirectoryType.TR, DirectoryType.TRP},
            TestRunPackageDirectoryDto.class,
            DirectoryType.TRP
    );

    private final String title;
    private final DirectoryType[] availableOptions;
    private final Class<?> targetDtoClass;
    private final DirectoryType targetParentType;

}