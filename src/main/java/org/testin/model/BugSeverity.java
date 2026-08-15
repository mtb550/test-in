package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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
    private final @NotNull String name;

}
