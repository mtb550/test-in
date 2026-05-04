package org.testin.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CreateNodeMenu {

    TEST_PROJECT(
            "Create Project",
            new DirectoryType[]{},
            DirectoryType.TP
    ),

    TEST_CASES_DIR(
            "Create Test Node",
            new DirectoryType[]{DirectoryType.TS, DirectoryType.TSP},
            DirectoryType.TCD
    ),

    TEST_RUNS_DIR(
            "Create Run Node",
            new DirectoryType[]{DirectoryType.TR, DirectoryType.TRP},
            DirectoryType.TRD
    ),

    TEST_SET_PACKAGE(
            "Create Test Node",
            new DirectoryType[]{DirectoryType.TS, DirectoryType.TSP},
            DirectoryType.TSP
    ),

    TEST_RUN_PACKAGE(
            "Create Run Node",
            new DirectoryType[]{DirectoryType.TR, DirectoryType.TRP},
            DirectoryType.TRP
    );

    private final String title;
    private final DirectoryType[] availableOptions;
    private final DirectoryType targetParentType;

    public static CreateNodeMenu fromDirectoryType(final DirectoryType type) {
        if (type == null) return TEST_PROJECT;

        for (CreateNodeMenu menu : values()) {
            if (menu.getTargetParentType() == type) {
                return menu;
            }
        }
        return TEST_PROJECT;
    }
}