package org.testin.testcase;

import org.testin.model.TestEditorAttributes;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.model.MenuItem;
import org.testin.model.StatusBarItem;
import org.testin.testcase.create.CreateTestCaseSection;
import org.testin.testcase.create.TestCaseBaseDialog;
import org.testin.testcase.update.bulk.DescriptionBulkSectionDialog;
import org.testin.testcase.update.bulk.ExpectedResultBulkSectionDialog;
import org.testin.testcase.update.bulk.GroupBulkSectionDialog;
import org.testin.testcase.update.bulk.ModuleBulkSectionDialog;
import org.testin.testcase.update.bulk.PreConditionsBulkSectionDialog;
import org.testin.testcase.update.bulk.PriorityBulkSectionDialog;
import org.testin.testcase.update.bulk.StatusBulkSectionDialog;
import org.testin.testcase.update.bulk.StepsBulkSectionDialog;
import org.testin.testcase.update.bulk.TestDataBulkSectionDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.function.Function;

import static org.testin.testcase.TestCaseDialogKey.*;

/**
 * A field the update menu offers: the section it opens, the bulk editor behind
 * it, and the generator that follows the change into the Java code.
 * <p>
 * Every constant here is a field. The keys a section advertises are
 * {@link TestCaseDialogKey}, shared with {@link CreateTestCaseFields} — the two
 * dialogs offer the same keys, and did so as two separate sets of constants
 * until the keys became a type of their own.
 */
@Getter
@AllArgsConstructor
public enum UpdateTestCaseFields implements MenuItem {

    DESCRIPTION(
            TestEditorAttributes.DESCRIPTION.getName(),
            Shortcuts.UpdateTestCaseDescription,
            AllIcons.Actions.Edit,
            GenType.UPDATE_TEST_CASE_DESCRIPTION,
            (p, items, updatedItems) -> new DescriptionBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getDescriptionSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    EXPECTED_RESULT(
            TestEditorAttributes.EXPECTED_RESULT.getName(),
            Shortcuts.UpdateTestCaseExpectedResult,
            AllIcons.General.InspectionsOK,
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            (p, items, updatedItems) -> new ExpectedResultBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getExpectedResultSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    MODULE(
            TestEditorAttributes.MODULE.getName(),
            Shortcuts.UpdateTestCaseModule,
            AllIcons.General.ContextHelp,
            GenType.UPDATE_TEST_CASE_MODULE,
            (p, items, updatedItems) -> new ModuleBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getModuleSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    TEST_DATA(
            TestEditorAttributes.TEST_DATA.getName(),
            Shortcuts.UpdateTestCaseTestData,
            AllIcons.Nodes.DataTables,
            GenType.UPDATE_TEST_CASE_TEST_DATA,
            (p, items, updatedItems) -> new TestDataBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getTestDataSection,
            new TestCaseDialogKey[]{}
    ),

    PRE_CONDITIONS(
            TestEditorAttributes.PRE_CONDITIONS.getName(),
            Shortcuts.UpdateTestCasePreConditions,
            AllIcons.Actions.StepOut,
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            (p, items, updatedItems) -> new PreConditionsBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getPreConditionsSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    STEPS(
            TestEditorAttributes.STEPS.getName(),
            Shortcuts.UpdateTestCaseSteps,
            AllIcons.Actions.ListFiles,
            GenType.UPDATE_TEST_CASE_STEPS,
            (p, items, updatedItems) -> new StepsBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getStepsSection,
            new TestCaseDialogKey[]{CORRECTIONS, ADD_STEP, NAVIGATE_TAB, AUTO_COMPLETE}
    ),

    PRIORITY(
            TestEditorAttributes.PRIORITY.getName(),
            Shortcuts.UpdateTestCasePriority,
            AllIcons.Nodes.Favorite,
            GenType.UPDATE_TEST_CASE_PRIORITY,
            (p, items, updatedItems) -> new PriorityBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getPrioritySection,
            new TestCaseDialogKey[]{NAVIGATE_ARROWS}
    ),

    GROUP(
            TestEditorAttributes.GROUP.getName(),
            Shortcuts.UpdateTestCaseGroup,
            AllIcons.Nodes.Tag,
            GenType.UPDATE_TEST_CASE_GROUP,
            (p, items, updatedItems) -> new GroupBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getGroupSection,
            new TestCaseDialogKey[]{ADD_GROUP, AUTO_COMPLETE, NAVIGATE_TAB}
    ),

    /**
     * The one field with no bulk form. A position is a place between two other
     * cases, and "move these eight to third" has no single meaning - so the
     * bulk action says so rather than guessing, which is the same answer the
     * tester would get from dragging eight cards onto one row.
     * <p>
     * Also the one field the create dialog does not offer: see
     * {@link org.testin.testcase.create.OrderSection}.
     */
    /**
     * UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194.
     * <p>
     * Where the case is in its own life, which is the one field here that no key
     * opens. The letters that would name it are taken by fields a tester reaches
     * far more often - `s` is Steps - and a status is not worth taking one from
     * them, so this is a menu row and nothing else.
     */
    STATUS(
            TestEditorAttributes.STATUS.getName(),
            Shortcuts.EMPTY,
            AllIcons.Actions.Preview,
            GenType.UPDATE_TEST_CASE_STATUS,
            (p, items, updatedItems) -> new StatusBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getStatusSection,
            new TestCaseDialogKey[]{}
    ),

    ORDER(
            TestEditorAttributes.ORDER.getName(),
            Shortcuts.UpdateTestCaseOrder,
            AllIcons.ObjectBrowser.Sorted,
            GenType.UPDATE_TEST_CASE_ORDER,
            (p, items, updatedItems) -> Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("update.order.one.at.a.time")),
            TestCaseBaseDialog::getOrderSection,
            new TestCaseDialogKey[]{}
    );

    private final @NotNull String name;
    private final @NotNull Shortcuts shortcut;
    private final @NotNull Icon icon;
    private final @NotNull GenType gt;
    private final @NotNull BulkEditorAction bulkAction;
    private final @NotNull Function<TestCaseBaseDialog, CreateTestCaseSection> sectionExtractor;

    /**
     * The keys this section adds to the shared ones.
     */
    private final TestCaseDialogKey @NotNull [] ownKeys;

    /**
     * What the section strip shows while this section holds the focus: its own
     * keys, and nothing else. Save and Cancel are on the strip below, which
     * never redraws (#56).
     */
    public StatusBarItem @NotNull [] getStatusBarItems() {
        return ownKeys.clone();
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }

    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        // A constant with no key binds nothing, which is what MenuItem promises.
        // Registering the empty keystroke would claim VK_UNDEFINED for whichever
        // field declared it first.
        if (Shortcuts.isNoKey(shortcut.getKey())) return;

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onTrigger.run();
            }
        }.registerCustomShortcutSet(shortcut.getCustomShortcut(), component);
    }
}
