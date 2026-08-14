package org.testin.enums;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;


@Getter
public enum Group {
    UNASSIGNED(
            "<No Group>",
            false,
            false
    ),

    REGRESSION(
            "Regression",
            true,
            true
    ),

    SMOKE(
            "Smoke",
            true,
            true
    ),

    SANITY(
            "Sanity",
            true,
            true
    ),

    SECURITY(
            "Security",
            false,
            true
    ),

    UI(
            "UI",
            false,
            true
    ),

    FUNCTIONAL(
            "Functional",
            false,
            true
    ),

    VALIDATION(
            "Validation",
            false,
            true
    );

    private final @NotNull String name;
    private final boolean active;
    private final boolean assignable;

    Group(final @NotNull String name, final boolean active, final boolean assignable) {
        this.name = name;
        this.active = active;
        this.assignable = assignable;
    }
}
