package org.testin.codegen;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
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
    REMOVE_TEST_PROJECT(
            "Remove Test Project",
            "Remove Automation Test Project"
    ),

    RENAME_TEST_PROJECT(
            "Rename Test Project",
            "Rename Automation Test Project"
    ),

    REMOVE_TEST_SET_PACKAGE(
            "Remove Test Set Package",
            "Remove Automation Test Package"
    ),

    RENAME_TEST_SET_PACKAGE(
            "Rename Test Set Package",
            "Rename Automation Test Package"
    ),

    MOVE_TEST_SET_PACKAGE(
            "Move Test Set Package",
            "Move Automation Test Package"
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

    MOVE_TEST_SET(
            "Move Test Set",
            "Move Automation Test Class"
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

    private final @NotNull String description;
    private final @NotNull String tooltip;

    /**
     * What this operation does to the generated code.
     * <p>
     * A value for every constant: the data-only attributes carry the no-op, and
     * everything else carries the resolver below. The resolver looks the real
     * generator up when it runs rather than here, which is what keeps this enum
     * free of PSI classes so it still loads in an IDE without Java support - and
     * is why the field can hold a value instead of a null standing for "look it
     * up later" (#71).
     */
    private final @NotNull GenAction action;

    GenType(final @NotNull String description, final @NotNull String tooltip) {
        this.description = description;
        this.tooltip = tooltip;
        this.action = this::runJavaAction;
    }

    GenType(final @NotNull String description, final @NotNull String tooltip, final @NotNull String dataOnlyField) {
        this.description = description;
        this.tooltip = tooltip;
        this.action = new NoOpCodeUpdate(dataOnlyField);
    }

    /**
     * Runs the Java-backed generator for this operation - or, in an IDE with no
     * Java plugin, says so once per project and skips quietly.
     */
    private void runJavaAction(final @NotNull Project p, final @NotNull Object obj) {
        if (!OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) return;

        GenRegistry.actionFor(this).execute(p, obj);
    }
}
