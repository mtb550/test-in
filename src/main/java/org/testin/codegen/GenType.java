package org.testin.codegen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.method.update.NoOpCodeUpdate;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;

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
            Bundle.message("codegen.remove.test.project"),
            "Remove Automation Test Project"
    ),

    RENAME_TEST_PROJECT(
            Bundle.message("codegen.rename.test.project"),
            "Rename Automation Test Project"
    ),

    REMOVE_TEST_SET_PACKAGE(
            Bundle.message("codegen.remove.test.set.package"),
            "Remove Automation Test Package"
    ),

    RENAME_TEST_SET_PACKAGE(
            Bundle.message("codegen.rename.test.set.package"),
            "Rename Automation Test Package"
    ),

    MOVE_TEST_SET_PACKAGE(
            Bundle.message("codegen.move.test.set.package"),
            "Move Automation Test Package"
    ),

    CREATE_TEST_SET(
            Bundle.message("codegen.create.test.set"),
            "Create Automation Test Class"
    ),

    REMOVE_TEST_SET(
            Bundle.message("codegen.remove.test.set"),
            "Remove Automation Test Class"
    ),

    RENAME_TEST_SET(
            Bundle.message("codegen.rename.test.set"),
            "Rename Automation Test Class"
    ),

    MOVE_TEST_SET(
            Bundle.message("codegen.move.test.set"),
            "Move Automation Test Class"
    ),

    CREATE_TEST_CASE(
            Bundle.message("codegen.create.test.case"),
            "Create Automation Test Method"
    ),

    REMOVE_TEST_CASE(
            Bundle.message("codegen.remove.test.case"),
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
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Description & Name"
    ),

    UPDATE_TEST_CASE_EXPECTED_RESULT(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Expected Result",
            "expected result"
    ),

    UPDATE_TEST_CASE_MODULE(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Module",
            "module"
    ),

    UPDATE_TEST_CASE_TEST_DATA(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Test Data",
            "test data"
    ),

    UPDATE_TEST_CASE_PRE_CONDITIONS(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Pre Conditions",
            "pre-conditions"
    ),

    UPDATE_TEST_CASE_STEPS(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Steps",
            "steps"
    ),

    UPDATE_TEST_CASE_GROUP(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Group"
    ),

    /**
     * Priority writes nothing into the code any more. The generated method's
     * priority attribute carries the case's position in its set, because that is
     * what decides execution order; the case's own High/Medium/Low is a Testin
     * field, shown and filtered and reported, and no concern of the automation.
     */
    UPDATE_TEST_CASE_PRIORITY(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Priority",
            "priority"
    ),

    /**
     * And order is what does write, for the same reason: a test framework runs
     * methods in the order the priority attribute gives them.
     */
    UPDATE_TEST_CASE_ORDER(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Order"
    ),

    /**
     * Whether the case runs at all. Disabled is the one status that says
     * anything about that, and it used to say it to Testin alone: the card
     * showed it, the JSON stored it, and the suite ran the case exactly as
     * before (#166).
     */
    /**
     * Everything Testin writes about a case, written again from the case.
     * <p>
     * For CTRL+Z, which restores the case and knows nothing about the code. A
     * snapshot is the case as it was rather than a list of what changed, so
     * there is no one field to update - this writes them all, which is right
     * whichever one moved.
     */
    RECONCILE_TEST_CASE(
            Bundle.message("codegen.restore.test.case"),
            "Restore Automation Test Method"
    ),

    UPDATE_TEST_CASE_STATUS(
            Bundle.message("codegen.update.test.case"),
            "Update Automation Test Method Enabled"
    ),

    /**
     * The attributes that never reach the Java: ids, paths, the audit fields.
     * A constant rather than a null on the attribute, so an edit runs its
     * generator either way instead of asking whether it has one.
     */
    NO_CODE_CHANGE(
            Bundle.message("codegen.no.code.change"),
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

        // UC-CODEGEN-019, Rule-CODEGEN-005
        @Override
        public void execute(final @NotNull Project p, final @NotNull Object obj) {
            if (!canGenerate(p)) return;

            CodeGenerators.find(GenType.this).execute(p, obj);
        }

        /**
         * UC-CODEGEN-019, Rule-CODEGEN-005, Rule-EDITOR-PANEL-046.
         * <p>
         * One command around the whole list, so one gesture is one entry on the
         * IDE's undo history.
         * <p>
         * Each generator used to open a command per case, so bulk-editing forty
         * descriptions was forty entries with the same name on them, and a
         * tester who changed their mind held CTRL+Z and watched the class
         * rewrite itself a method at a time with no way to tell how many
         * presses were left. Dragging one card in a set of two hundred was
         * worse: the order sweep touches every case in the set (#153).
         * <p>
         * The hop is here rather than in each generator because a command has
         * to be opened on the EDT and a caller may not be on it - the update
         * menu hands its list over from a pooled thread. A generator that opens
         * one of its own is merged into this one rather than starting a second,
         * which is what lets the two that batch by class keep doing it.
         */
        @Override
        public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
            if (!canGenerate(p) || items.isEmpty()) return;

            ApplicationManager.getApplication().invokeLater(() ->
                    WriteCommandAction.runWriteCommandAction(p, description, null,
                            () -> CodeGenerators.find(GenType.this).executeAll(p, items)));
        }

        /**
         * UC-CODEGEN-019, Rule-CODEGEN-005, Rule-CODEGEN-006.
         * <p>
         * Whether there is anything that can generate right now. Two questions,
         * and both belong here rather than in the fourteen generators: an IDE
         * without the Java plugin has nothing to run, and an IDE still building
         * its index cannot look a class up by name.
         * <p>
         * Every generator resolves its target through
         * {@code JavaPsiFacade.findClass}, which raises rather than answering
         * empty while the index is being built - and every action that reaches
         * one is a {@code DumbAwareAction}, so they are all live during
         * indexing. The two together are an internal error in front of a tester
         * who was creating a test case (#126).
         */
        private boolean canGenerate(final @NotNull Project p) {
            if (!OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) return false;
            if (!DumbService.isDumb(p)) return true;

            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.WHILE_INDEXING, description);
            Logger.info("Skipped " + tooltip + ": the IDE is indexing");
            return false;
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
