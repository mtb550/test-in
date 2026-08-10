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
import org.testin.util.Shortcuts;
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
            new IStatusBarItem[]{SAVE},
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
            new IStatusBarItem[]{SAVE},
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
            new IStatusBarItem[]{SAVE},
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
            new IStatusBarItem[]{SAVE},
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
            new IStatusBarItem[]{SAVE},
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
            new IStatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, NAVIGATE_TAB, AUTO_COMPLETE},
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
            new IStatusBarItem[]{SAVE, NAVIGATE_ARROWS, SET_PRIORITY},
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
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            GeneratorType.UPDATE_TEST_CASE_GROUP,
            (p, items, updatedItems) -> new GroupBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getGroupSection
    );

    private final String name;
    private final Shortcuts shortcut;
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