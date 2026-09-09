package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;


/**
 * The groups a test case can be filed under, and the placeholder for none.
 * <p>
 * Every one of them can be typed, filtered on, imported and exported. It was
 * not so: an {@code active} flag kept four of them - Security, UI, Functional
 * and Validation - out of the create and update dialogs while the filter, the
 * import picker and the parser all took them. So a test case could arrive
 * carrying a group no tester could have typed, and did, through an import, a
 * paste, a Git merge or a bulk edit (#200).
 * <p>
 * There was a second flag, {@code assignable}, read by nothing at all. It was
 * false for {@link #UNASSIGNED} and true for the seven real groups, which is
 * what the constant's own identity says - and every reader already asks it that
 * way.
 * <p>
 * The order is the declaration's: No Group first, then the groups. The dialog,
 * the filter and the import picker all walk {@code values()}, so they cannot
 * disagree about which comes first.
 */
@Getter
@AllArgsConstructor
public enum Group {
    UNASSIGNED("<No Group>"),

    REGRESSION("Regression"),

    SMOKE("Smoke"),

    SANITY("Sanity"),

    SECURITY("Security"),

    UI("UI"),

    FUNCTIONAL("Functional"),

    VALIDATION("Validation");

    private final @NotNull String name;
}
