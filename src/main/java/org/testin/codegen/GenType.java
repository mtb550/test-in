package org.testin.codegen;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.method.update.NoOpCodeUpdate;
import org.testin.util.OptionalPlugin;

/**
 * Every automation-code operation the plugin can perform. Constants carry no
 * PSI-dependent classes: Java-backed actions are resolved lazily through
 * {@link GenRegistry}, which is only class-loaded when the Java plugin
 * is available — so this enum is safe to load in IDEs without Java support
 * (PyCharm, GoLand, WebStorm, ...).
 */
@Getter
public enum GenType {
    CREATE_TEST_PROJECT(
            "Create Test Project",
            "Create Automation Test Project"
    ),

    RENAME_TEST_PROJECT(
            "Rename Test Project",
            "Rename Automation Test Project"
    ),

    CREATE_TEST_SET_PACKAGE(
            "Create Test Set Package",
            "Create Automation Test Package"
    ),

    REMOVE_TEST_SET_PACKAGE(
            "Remove Test Set Package",
            "Remove Automation Test Package"
    ),

    RENAME_TEST_SET_PACKAGE(
            "Rename Test Set Package",
            "Rename Automation Test Package"
    ),

    CREATE_TEST_SET(
            "Create Test Set",
            "Create Automation Test Class"
    ),

    REMOVE_TEST_SET(
            "Remove Test Set",
            "Remove Automation Test Class"
    ),

    RENAME_TEST_SET(
            "Rename Test Set",
            "Rename Automation Test Class"
    ),

    CREATE_TEST_CASE(
            "Create Test Case",
            "Create Automation Test Method"
    ),

    REMOVE_TEST_CASE(
            "Remove Test Case",
            "Remove Automation Test Method"
    ),

    RENAME_TEST_CASE(
            "Rename Test Case",
            "Rename Automation Test Method"
    ),

    UPDATE_TEST_CASE_DESCRIPTION(
            "Update Test Case",
            "Update Automation Test Method Description & Name"
    ),

    UPDATE_TEST_CASE_EXPECTED_RESULT(
            "Update Test Case",
            "Update Automation Test Method Expected Result",
            "expected result"
    ),

    UPDATE_TEST_CASE_MODULE(
            "Update Test Case",
            "Update Automation Test Method Module",
            "module"
    ),

    UPDATE_TEST_CASE_TEST_DATA(
            "Update Test Case",
            "Update Automation Test Method Test Data",
            "test data"
    ),

    UPDATE_TEST_CASE_PRE_CONDITIONS(
            "Update Test Case",
            "Update Automation Test Method Pre Conditions",
            "pre-conditions"
    ),

    UPDATE_TEST_CASE_STEPS(
            "Update Test Case",
            "Update Automation Test Method Steps",
            "steps"
    ),

    UPDATE_TEST_CASE_GROUP(
            "Update Test Case",
            "Update Automation Test Method Group"
    ),

    UPDATE_TEST_CASE_PRIORITY(
            "Update Test Case",
            "Update Automation Test Method Priority"
    ),

    UPDATE_TEST_CASE_ORDER(
            "Update Test Case",
            "Update Automation Test Method Order",
            "order"
    ),

    /**
     * The attributes that never reach the Java: ids, paths, the audit fields.
     * A constant rather than a null on the attribute, so an edit runs its
     * generator either way instead of asking whether it has one.
     */
    NO_CODE_CHANGE(
            "No Code Change",
            "This attribute has no generated code",
            "read-only attribute"
    );

    /**
     * Returned when the Java plugin is absent: notify once per project, then skip quietly.
     */
    private static final @NotNull GenAction JAVA_UNAVAILABLE = (p, obj) -> OptionalPlugin.JAVA.isAvailableOrWarnOnce(p);

    private final @NotNull String description;
    private final @NotNull String tooltip;

    /**
     * Set for data-only fields that never change generated code; null for Java-backed actions.
     */
    @Getter(AccessLevel.NONE)
    private final @Nullable GenAction dataOnlyAction;

    GenType(final @NotNull String description, final @NotNull String tooltip) {
        this.description = description;
        this.tooltip = tooltip;
        this.dataOnlyAction = null;
    }

    GenType(final @NotNull String description, final @NotNull String tooltip, final @NotNull String dataOnlyField) {
        this.description = description;
        this.tooltip = tooltip;
        this.dataOnlyAction = new NoOpCodeUpdate(dataOnlyField);
    }

    public @NotNull GenAction getAction() {
        if (dataOnlyAction != null) return dataOnlyAction;
        if (!OptionalPlugin.JAVA.isAvailable()) return JAVA_UNAVAILABLE;
        return GenRegistry.actionFor(this);
    }
}
