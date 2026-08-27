package org.testin.codegen;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.method.update.NoOpCodeUpdate;
import org.testin.util.OptionalPlugin;

import java.util.List;

/**
 * Every automation-code operation the plugin can perform. Constants carry no
 * PSI-dependent classes: Java-backed actions are resolved lazily through
 * {@link CodeGenerators}, an extension point a content module contributes to -
 * so this enum is safe to load in IDEs without Java support (PyCharm, GoLand,
 * WebStorm, ...), and the classes that do the work are not in the core jar at
 * all (#144).
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

    /**
     * Renaming the generated method is part of this, not a step beside it: the
     * method is named after the case's description, so a description that
     * changed and a method that did not are the same edit half done.
     * <p>
     * There was a RENAME_TEST_CASE beside this one, with a handler that renamed
     * and nothing else. Nothing ever dispatched it, and if anything had it would
     * have left the @Test description saying what the case used to say.
     */
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

    /**
     * Priority writes nothing into the code any more. The generated method's
     * priority attribute carries the case's position in its set, because that is
     * what decides execution order; the case's own High/Medium/Low is a Testin
     * field, shown and filtered and reported, and no concern of the automation.
     */
    UPDATE_TEST_CASE_PRIORITY(
            "Update Test Case",
            "Update Automation Test Method Priority",
            "priority"
    ),

    /**
     * And order is what does write, for the same reason: a test framework runs
     * methods in the order the priority attribute gives them.
     */
    UPDATE_TEST_CASE_ORDER(
            "Update Test Case",
            "Update Automation Test Method Order"
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
        this.action = new JavaCodeUpdate();
    }

    GenType(final @NotNull String description, final @NotNull String tooltip, final @NotNull String dataOnlyField) {
        this.description = description;
        this.tooltip = tooltip;
        this.action = new NoOpCodeUpdate(dataOnlyField);
    }

    /**
     * Runs the Java-backed generator for this operation - or, in an IDE with no
     * Java plugin, says so once per project and skips quietly.
     * <p>
     * A class rather than the method reference it used to be, so that it can
     * answer for a list as well as for one item. A lambda cannot: it takes the
     * interface's default {@code executeAll}, which is a loop, and the whole
     * point of handing a generator the set is that it can do the per-class work
     * once instead of per case.
     */
    private final class JavaCodeUpdate implements GenAction {

        @Override
        public void execute(final @NotNull Project p, final @NotNull Object obj) {
            if (!OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) return;

            CodeGenerators.find(GenType.this).execute(p, obj);
        }

        @Override
        public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
            if (!OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) return;

            CodeGenerators.find(GenType.this).executeAll(p, items);
        }
    }

    /**
     * Generates for a whole list in one go. A caller with a set in hand - an
     * import, a copied test set, a bulk edit - hands the set over rather than
     * the cases one by one, so the generator can do the work that is per class
     * once instead of per case.
     * <p>
     * Through the same action {@link #getAction()} returns, which it did not
     * used to be: it went straight to the registry, so a data-only attribute
     * asked for a Java generator it has no use for and warned about the missing
     * Java plugin on the way. The two forms of the same operation now behave the
     * same, because they are the same object.
     */
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        action.executeAll(p, items);
    }
}
