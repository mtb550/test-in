package org.testin.enums;

import com.intellij.icons.AllIcons;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.statusBar.IStatusBarItem;
import org.testin.testCase.createDialog.ICreateTestCaseSection;
import org.testin.testCase.createDialog.TestCaseBaseDialog;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.function.Function;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CreateTestCaseFields implements IStatusBarItem {
    DESCRIPTION_SHORTCUT(
            "Description",
            Shortcuts.CreateTestCaseDescription,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    EXPECTED_RESULT_SHORTCUT(
            "Expected Result",
            Shortcuts.CreateTestCaseExpectedResult,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    STEPS_SHORTCUT(
            "Steps",
            Shortcuts.CreateTestCaseAddStep,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    PRIORITY_SHORTCUT(
            "Priority",
            Shortcuts.CreateTestCasePriority,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    GROUP_SHORTCUT(
            "Groups",
            Shortcuts.CreateTestCaseGroup,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    SAVE(
            "Save",
            Shortcuts.Enter,
            null,
            null,
            new IStatusBarItem[]{},
            false,
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
            null
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            null,
            Shortcuts.AutoComplete.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    SET_PRIORITY(
            "Set Priority",
            null,
            Shortcuts.PriorityHigh.getShortcutText() + " / " + Shortcuts.PriorityMedium.getShortcutText() + " / " + Shortcuts.PriorityLow.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
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
            null
    ),

    DESCRIPTION(
            "Description",
            Shortcuts.CreateTestCaseDescription,
            null,
            AllIcons.Actions.Edit,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB, DESCRIPTION_SHORTCUT, EXPECTED_RESULT_SHORTCUT, STEPS_SHORTCUT, PRIORITY_SHORTCUT, GROUP_SHORTCUT},
            true,
            TestCaseBaseDialog::getDescriptionSection,
            "set description"
    ),

    EXPECTED_RESULT(
            "Expected Results",
            Shortcuts.CreateTestCaseExpectedResult,
            null,
            AllIcons.General.InspectionsOK,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseBaseDialog::getExpectedResultSection,
            "set expected result"
    ),

    MODULE(
            "Module",
            Shortcuts.CreateTestCaseModule,
            null,
            AllIcons.General.ContextHelp,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseBaseDialog::getModuleSection,
            "set module"
    ),

    TEST_DATA(
            "Test Data",
            Shortcuts.CreateTestCaseTestData,
            null,
            AllIcons.Nodes.DataTables,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseBaseDialog::getTestDataSection,
            "set test data"
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            Shortcuts.CreateTestCasePreConditions,
            null,
            AllIcons.Actions.StepOut,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseBaseDialog::getPreConditionsSection,
            "set pre conditions"
    ),

    STEPS(
            "Steps",
            Shortcuts.CreateTestCaseAddStep,
            null,
            AllIcons.Actions.ListFiles,
            new IStatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, AUTO_COMPLETE, NAVIGATE_TAB},
            true,
            TestCaseBaseDialog::getStepsSection,
            "set step"
    ),

    PRIORITY(
            "Priority",
            Shortcuts.CreateTestCasePriority,
            null,
            AllIcons.Nodes.Favorite,
            new IStatusBarItem[]{SAVE, SET_PRIORITY, NAVIGATE_ARROWS},
            true,
            TestCaseBaseDialog::getPrioritySection,
            null
    ),

    SELECT_GROUP(
            "Select / Unselect Group",
            Shortcuts.SelectGroup,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    GROUP(
            "Group",
            Shortcuts.CreateTestCaseGroup,
            null,
            AllIcons.Nodes.Tag,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            TestCaseBaseDialog::getGroupSection,
            null
    );

    private final String name;
    private final Shortcuts shortcut;
    private final String customShortcutText;
    private final Icon icon;
    private final IStatusBarItem[] statusBarItems;
    private final boolean createMenuItem;
    private final Function<TestCaseBaseDialog, ICreateTestCaseSection> sectionExtractor;
    private final String placeholder;

    // todo, to be removed.
    @Override
    public String getShortcutText() {
        if (customShortcutText != null)
            return customShortcutText;

        return shortcut != null ? shortcut.getShortcutText() : "";
    }
}