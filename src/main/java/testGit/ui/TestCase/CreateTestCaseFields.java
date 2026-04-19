package testGit.ui.TestCase;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import testGit.util.KeyboardSet;
import testGit.util.statusBar.IStatusBarItem;

import javax.swing.*;
import java.util.function.Function;

@Getter
public enum CreateTestCaseFields implements IStatusBarItem {
    SAVE(
            "Save",
            KeyboardSet.Enter,
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    ADD_STEP(
            "Add Step",
            KeyboardSet.CreateTestCaseAddStep,
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    REMOVE_STEP(
            "Remove Step",
            KeyboardSet.CreateTestCaseRemoveStep,
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            KeyboardSet.AutoComplete.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    SET_PRIORITY(
            "Set Priority",
            KeyboardSet.PriorityHigh.getShortcutText() + " / " + KeyboardSet.PriorityMedium.getShortcutText() + " / " + KeyboardSet.PriorityLow.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    NAVIGATE_TAB(
            "Navigate",
            KeyboardSet.TabNext.getShortcutText() + " / " + KeyboardSet.TabPrevious.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    NAVIGATE_ARROWS(
            "Navigate Priority",
            KeyboardSet.ArrowUp.getShortcutText() + " / " + KeyboardSet.ArrowDown.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    DESCRIPTION(
            "Description",
            KeyboardSet.CreateTestCaseTitle,
            AllIcons.Actions.Edit,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getDescriptionSection
    ),

    EXPECTED_RESULT(
            "Expected Results",
            KeyboardSet.CreateTestCaseExpected,
            AllIcons.General.InspectionsOK,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getExpectedResultSection
    ),

    STEPS(
            "Steps",
            KeyboardSet.CreateTestCaseAddStep,
            AllIcons.Actions.ListFiles,
            new IStatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, AUTO_COMPLETE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getStepsSection
    ),

    PRIORITY(
            "Priority",
            KeyboardSet.CreateTestCasePriority,
            AllIcons.Nodes.Favorite,
            new IStatusBarItem[]{SAVE, SET_PRIORITY, NAVIGATE_ARROWS},
            true,
            TestCaseUIBase::getPrioritySection
    ),

    SELECT_GROUP(
            "Select / Unselect Group",
            KeyboardSet.SelectGroup,
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    GROUP(
            "Group",
            KeyboardSet.CreateTestCaseGroup,
            AllIcons.Nodes.Tag,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            TestCaseUIBase::getGroupSection
    );

    private final String name;
    private final KeyboardSet shortcut;
    private final String customShortcutText;
    private final Icon icon;
    private final IStatusBarItem[] statusBarItems;
    private final boolean createMenuItem;
    private final Function<TestCaseUIBase, ICreateTestCaseSection> sectionExtractor;

    CreateTestCaseFields(final String name, final KeyboardSet shortcut, final Icon icon, final IStatusBarItem[] statusBarItems, final boolean createMenuItem, final Function<TestCaseUIBase, ICreateTestCaseSection> sectionExtractor) {
        this.name = name;
        this.shortcut = shortcut;
        this.customShortcutText = null;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.createMenuItem = createMenuItem;
        this.sectionExtractor = sectionExtractor;
    }

    CreateTestCaseFields(final String name, final String customShortcutText, final Icon icon, final IStatusBarItem[] statusBarItems, final boolean createMenuItem, final Function<TestCaseUIBase, ICreateTestCaseSection> sectionExtractor) {
        this.name = name;
        this.shortcut = null;
        this.customShortcutText = customShortcutText;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.createMenuItem = createMenuItem;
        this.sectionExtractor = sectionExtractor;
    }

    @Override
    public String getShortcutText() {
        if (customShortcutText != null) {
            return customShortcutText;
        }
        return shortcut != null ? shortcut.getShortcutText() : "";
    }
}