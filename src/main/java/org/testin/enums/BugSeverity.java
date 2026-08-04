package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum BugSeverity {
    EMPTY(
            ""
    ),

    BLOCKER(
            "Blocker"
    ),

    MAJOR(
            "Major"
    ),

    MINOR(
            "Minor"
    ),

    ENHANCEMENT(
            "Enhancement"
    );

    @Getter
    private final String name;

}