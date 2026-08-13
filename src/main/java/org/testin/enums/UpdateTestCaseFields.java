package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.GeneratorType;
import org.testin.statusBar.IStatusBarItem;
import org.testin.testCase.createDialog.ICreateTestCaseSection;
import org.testin.testCase.createDialog.TestCaseBaseDialog;
import org.testin.testCase.updateDialog.bulk.*;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum UpdateTestCaseFields implements IStatusBarItem {
    SAVE(
            "Save",
            Shortcuts.Enter,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    CANCEL(
            "Cancel",
            Shortcuts.Escape,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    // Display only: the platform binds Alt+Enter on the editors itself. Shown
    // only on the fields that actually spell check.
    CORRECTIONS(
            "Corrections",
            null,
            Shortcuts.Corrections.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    ADD_STEP(
            "Add Step",
            Shortcuts.CreateTestCaseAddStep,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    REMOVE_STEP(
            "Remove Step",
            Shortcuts.CreateTestCaseRemoveStep,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    NAVIGATE_TAB(
            "Navigate",
            null,
            Shortcuts.TabNext.getShortcutText() + " / " + Shortcuts.TabPrevious.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    NAVIGATE_ARROWS(
            "Navigate Priority",
            null,
            Shortcuts.ArrowUp.getShortcutText() + " / " + Shortcuts.ArrowDown.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    DESCRIPTION(
            "Description",
            Shortcuts.UpdateTestCaseDescription,
            null,
            AllIcons.Actions.Edit,
            new IStatusBarItem[]{SAVE, CANCEL, CORRECTIONS},
            true,
            GeneratorType.UPDATE_TEST_CASE_DESCRIPTION,
            (p, items, updatedItems) -> new DescriptionBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getDescriptionSection
    ),

    EXPECTED_RESULT(
            "Expected Results",
            Shortcuts.UpdateTestCaseExpectedResult,
            null,
            AllIcons.General.InspectionsOK,
            new IStatusBarItem[]{SAVE, CANCEL, CORRECTIONS},
            true,
            GeneratorType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            (p, items, updatedItems) -> new ExpectedResultBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getExpectedResultSection
    ),

    MODULE(
            "Module",
            Shortcuts.UpdateTestCaseModule,
            null,
            AllIcons.General.ContextHelp,
            new IStatusBarItem[]{SAVE, CANCEL, CORRECTIONS},
            true,
            GeneratorType.UPDATE_TEST_CASE_MODULE,
            (p, items, updatedItems) -> new ModuleBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getModuleSection
    ),

    TEST_DATA(
            "Test Data",
            Shortcuts.UpdateTestCaseTestData,
            null,
            AllIcons.Nodes.DataTables,
            new IStatusBarItem[]{SAVE, CANCEL},
            true,
            GeneratorType.UPDATE_TEST_CASE_TEST_DATA,
            (p, items, updatedItems) -> new TestDataBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getTestDataSection
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            Shortcuts.UpdateTestCasePreConditions,
            null,
            AllIcons.Actions.StepOut,
            new IStatusBarItem[]{SAVE, CANCEL, CORRECTIONS},
            true,
            GeneratorType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            (p, items, updatedItems) -> new PreConditionsBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getPreConditionsSection
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            null,
            Shortcuts.AutoComplete.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    STEPS(
            "Steps",
            Shortcuts.UpdateTestCaseSteps,
            null,
            AllIcons.Actions.ListFiles,
            new IStatusBarItem[]{SAVE, CANCEL, CORRECTIONS, ADD_STEP, REMOVE_STEP, NAVIGATE_TAB, AUTO_COMPLETE},
            true,
            GeneratorType.UPDATE_TEST_CASE_STEPS,
            (p, items, updatedItems) -> new StepsBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getStepsSection
    ),

    SET_PRIORITY(
            "Set Priority",
            null,
            Shortcuts.PriorityHigh.getShortcutText() + " / " + Shortcuts.PriorityMedium.getShortcutText() + " / " + Shortcuts.PriorityLow.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    PRIORITY(
            "Priority",
            Shortcuts.UpdateTestCasePriority,
            null,
            AllIcons.Nodes.Favorite,
            new IStatusBarItem[]{SAVE, CANCEL, NAVIGATE_ARROWS, SET_PRIORITY},
            true,
            GeneratorType.UPDATE_TEST_CASE_PRIORITY,
            (p, items, updatedItems) -> new PriorityBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getPrioritySection
    ),

    SELECT_GROUP(
            "Select / Unselect Group",
            Shortcuts.SelectGroup,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    GROUP(
            "Group",
            Shortcuts.UpdateTestCaseGroup,
            null,
            AllIcons.Nodes.Tag,
            new IStatusBarItem[]{SAVE, CANCEL, NAVIGATE_TAB, SELECT_GROUP},
            true,
            GeneratorType.UPDATE_TEST_CASE_GROUP,
            (p, items, updatedItems) -> new GroupBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getGroupSection
    );

    private final @NotNull String name;

    // The action-only constants (Save, Add Step, ...) are status-bar hints:
    // they name a key and carry no icon, no code generator and no dialog
    // section of their own.
    private final @Nullable Shortcuts shortcut;
    private final @Nullable String customShortcutText;
    private final @Nullable Icon icon;
    private final IStatusBarItem @NotNull [] statusBarItems;

    /**
     * True for the constants the update menu offers. Those - and only those -
     * carry an icon, a generator type, a bulk action and a dialog section; the
     * rest are status-bar hints for keys the sections bind themselves. Use the
     * {@code require*} accessors below rather than trusting a caller to have
     * filtered on this flag first.
     */
    private final boolean updateMenuItem;
    private final @Nullable GeneratorType gt;
    private final @Nullable IBulkEditorAction bulkAction;
    private final @Nullable Function<TestCaseBaseDialog, ICreateTestCaseSection> sectionExtractor;

    public @NotNull GeneratorType requireGt() {
        return require(gt, "generator type");
    }

    public @NotNull IBulkEditorAction requireBulkAction() {
        return require(bulkAction, "bulk action");
    }

    public @NotNull Function<TestCaseBaseDialog, ICreateTestCaseSection> requireSectionExtractor() {
        return require(sectionExtractor, "dialog section");
    }

    private <T> @NotNull T require(final @Nullable T value, final @NotNull String what) {
        if (value == null) {
            throw new IllegalStateException(name + " has no " + what + ": it is not an updatable field");
        }
        return value;
    }

    @Override
    public @NotNull String getShortcutText() {
        if (customShortcutText != null) {
            return customShortcutText;
        }
        return shortcut != null ? shortcut.getShortcutText() : "";
    }

    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        if (this.shortcut != null) {
            new DumbAwareAction() {
                @Override
                public void actionPerformed(final @NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                    onTrigger.run();
                }
            }.registerCustomShortcutSet(this.shortcut.getCustomShortcut(), component);
        }
    }

}
