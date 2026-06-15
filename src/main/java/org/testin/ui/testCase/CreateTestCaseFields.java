package org.testin.ui.testCase;

import com.intellij.icons.AllIcons;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.util.KeyboardSet;
import org.testin.util.statusBar.IStatusBarItem;

import javax.swing.*;
import java.util.function.Function;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CreateTestCaseFields implements IStatusBarItem {
    DESCRIPTION_SHORTCUT(
            "Description",
            KeyboardSet.CreateTestCaseDescription,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    EXPECTED_RESULT_SHORTCUT(
            "Expected Result",
            KeyboardSet.CreateTestCaseExpectedResult,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    STEPS_SHORTCUT(
            "Steps",
            KeyboardSet.CreateTestCaseAddStep,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    PRIORITY_SHORTCUT(
            "Priority",
            KeyboardSet.CreateTestCasePriority,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    GROUP_SHORTCUT(
            "Groups",
            KeyboardSet.CreateTestCaseGroup,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    SAVE(
            "Save",
            KeyboardSet.Enter,
            null,
            null,
            new IStatusBarItem[]{},
            false,
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
            null
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            null,
            KeyboardSet.AutoComplete.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    SET_PRIORITY(
            "Set Priority",
            null,
            KeyboardSet.PriorityHigh.getShortcutText() + " / " + KeyboardSet.PriorityMedium.getShortcutText() + " / " + KeyboardSet.PriorityLow.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
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
            null
    ),

    DESCRIPTION(
            "Description",
            KeyboardSet.CreateTestCaseDescription,
            null,
            AllIcons.Actions.Edit,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB, DESCRIPTION_SHORTCUT, EXPECTED_RESULT_SHORTCUT, STEPS_SHORTCUT, PRIORITY_SHORTCUT, GROUP_SHORTCUT},
            true,
            TestCaseUIBase::getDescriptionSection,
            "set description"
    ),

    EXPECTED_RESULT(
            "Expected Results",
            KeyboardSet.CreateTestCaseExpectedResult,
            null,
            AllIcons.General.InspectionsOK,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getExpectedResultSection,
            "set expected result"
    ),

    MODULE(
            "Module",
            KeyboardSet.CreateTestCaseModule,
            null,
            AllIcons.General.ContextHelp,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getModuleSection,
            "set module"
    ),

    TEST_DATA(
            "Test Data",
            KeyboardSet.CreateTestCaseTestData,
            null,
            AllIcons.Nodes.DataTables,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getTestDataSection,
            "set test data"
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            KeyboardSet.CreateTestCasePreConditions,
            null,
            AllIcons.Actions.StepOut,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getPreConditionsSection,
            "set pre conditions"
    ),

    STEPS(
            "Steps",
            KeyboardSet.CreateTestCaseAddStep,
            null,
            AllIcons.Actions.ListFiles,
            new IStatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, AUTO_COMPLETE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getStepsSection,
            "set step"
    ),

    PRIORITY(
            "Priority",
            KeyboardSet.CreateTestCasePriority,
            null,
            AllIcons.Nodes.Favorite,
            new IStatusBarItem[]{SAVE, SET_PRIORITY, NAVIGATE_ARROWS},
            true,
            TestCaseUIBase::getPrioritySection,
            null
    ),

    SELECT_GROUP(
            "Select / Unselect Group",
            KeyboardSet.SelectGroup,
            null,
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    GROUP(
            "Group",
            KeyboardSet.CreateTestCaseGroup,
            null,
            AllIcons.Nodes.Tag,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            TestCaseUIBase::getGroupSection,
            null
    );

    private final String name;
    private final KeyboardSet shortcut;
    private final String customShortcutText;
    private final Icon icon;
    private final IStatusBarItem[] statusBarItems;
    private final boolean createMenuItem;
    private final Function<TestCaseUIBase, ICreateTestCaseSection> sectionExtractor;
    private final String placeholder;

    // todo, to be removed.
    @Override
    public String getShortcutText() {
        if (customShortcutText != null)
            return customShortcutText;

        return shortcut != null ? shortcut.getShortcutText() : "";
    }
}