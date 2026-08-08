package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.statusBar.IStatusBarItem;
import org.testin.testCase.createDialog.ICreateTestCaseSection;
import org.testin.testCase.createDialog.TestCaseBaseDialog;
import org.testin.testCase.updateDialog.bulk.*;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum UpdateTestCaseFields implements IStatusBarItem {
    SAVE(
            "Save",
            KeyboardSet.Enter,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    ADD_STEP(
            "Add Step",
            KeyboardSet.CreateTestCaseAddStep,
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
            KeyboardSet.CreateTestCaseRemoveStep,
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
            KeyboardSet.TabNext.getShortcutText() + " / " + KeyboardSet.TabPrevious.getShortcutText(),
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
            KeyboardSet.ArrowUp.getShortcutText() + " / " + KeyboardSet.ArrowDown.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    DESCRIPTION(
            "Description",
            KeyboardSet.UpdateTestCaseDescription,
            null,
            AllIcons.Actions.Edit,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_DESCRIPTION,
            (p, items, updatedItems) -> new DescriptionBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getDescriptionSection
    ),

    EXPECTED_RESULT(
            "Expected Results",
            KeyboardSet.UpdateTestCaseExpectedResult,
            null,
            AllIcons.General.InspectionsOK,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            (p, items, updatedItems) -> new ExpectedResultBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getExpectedResultSection
    ),

    MODULE(
            "Module",
            KeyboardSet.UpdateTestCaseModule,
            null,
            AllIcons.General.ContextHelp,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_MODULE,
            (p, items, updatedItems) -> new ModuleBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getModuleSection
    ),

    TEST_DATA(
            "Test Data",
            KeyboardSet.UpdateTestCaseTestData,
            null,
            AllIcons.Nodes.DataTables,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_TEST_DATA,
            (p, items, updatedItems) -> new TestDataBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getTestDataSection
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            KeyboardSet.UpdateTestCasePreConditions,
            null,
            AllIcons.Actions.StepOut,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            (p, items, updatedItems) -> new PreConditionsBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getPreConditionsSection
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            null,
            KeyboardSet.AutoComplete.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    STEPS(
            "Steps",
            KeyboardSet.UpdateTestCaseSteps,
            null,
            AllIcons.Actions.ListFiles,
            new IStatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, NAVIGATE_TAB, AUTO_COMPLETE},
            true,
            GeneratorType.UPDATE_TEST_CASE_STEPS,
            (p, items, updatedItems) -> new StepsBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getStepsSection
    ),

    SET_PRIORITY(
            "Set Priority",
            null,
            KeyboardSet.PriorityHigh.getShortcutText() + " / " + KeyboardSet.PriorityMedium.getShortcutText() + " / " + KeyboardSet.PriorityLow.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null,
            null
    ),

    PRIORITY(
            "Priority",
            KeyboardSet.UpdateTestCasePriority,
            null,
            AllIcons.Nodes.Favorite,
            new IStatusBarItem[]{SAVE, NAVIGATE_ARROWS, SET_PRIORITY},
            true,
            GeneratorType.UPDATE_TEST_CASE_PRIORITY,
            (p, items, updatedItems) -> new PriorityBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getPrioritySection
    ),

    SELECT_GROUP(
            "Select / Unselect Group",
            KeyboardSet.SelectGroup,
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
            KeyboardSet.UpdateTestCaseGroup,
            null,
            AllIcons.Nodes.Tag,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            GeneratorType.UPDATE_TEST_CASE_GROUP,
            (p, items, updatedItems) -> new GroupBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getGroupSection
    );

    private final String name;
    private final KeyboardSet shortcut;
    private final String customShortcutText;
    private final Icon icon;
    private final IStatusBarItem[] statusBarItems;
    private final boolean updateMenuItem;
    private final GeneratorType gt;
    private final IBulkEditorAction bulkAction;
    private final Function<TestCaseBaseDialog, ICreateTestCaseSection> sectionExtractor;


    @Override
    public String getShortcutText() {
        if (customShortcutText != null) {
            return customShortcutText;
        }
        return shortcut != null ? shortcut.getShortcutText() : "";
    }

    public void bindShortcut(final JComponent component, final Runnable onTrigger) {
        if (this.shortcut != null) {
            new DumbAwareAction() {
                @Override
                public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                    onTrigger.run();
                }
            }.registerCustomShortcutSet(this.shortcut.getCustomShortcut(), component);
        }
    }

}