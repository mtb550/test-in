package testGit.ui.TestCase;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import testGit.util.KeyboardSet;
import testGit.util.statusBar.StatusBarItem;

import javax.swing.*;
import java.util.function.Function;

@Getter
public enum CreateField implements StatusBarItem {
    SAVE(
            "Save",
            KeyboardSet.Enter,
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    ADD_STEP(
            "Add Step",
            KeyboardSet.CreateTestCaseAddStep,
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    REMOVE_STEP(
            "Remove Step",
            KeyboardSet.CreateTestCaseRemoveStep,
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            KeyboardSet.AutoComplete.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    SET_PRIORITY(
            "Set Priority",
            KeyboardSet.PriorityHigh.getShortcutText() + " / " + KeyboardSet.PriorityMedium.getShortcutText() + " / " + KeyboardSet.PriorityLow.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    NAVIGATE_TAB(
            "Navigate",
            KeyboardSet.TabNext.getShortcutText() + " / " + KeyboardSet.TabPrevious.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    NAVIGATE_ARROWS(
            "Navigate Priority",
            KeyboardSet.ArrowUp.getShortcutText() + " / " + KeyboardSet.ArrowDown.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    TITLE(
            "Title",
            KeyboardSet.CreateTestCaseTitle,
            AllIcons.Actions.Edit,
            new StatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getTitleSection
    ),

    EXPECTED(
            "Expected Results",
            KeyboardSet.CreateTestCaseExpected,
            AllIcons.General.InspectionsOK,
            new StatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getExpectedSection
    ),

    STEPS(
            "Steps",
            KeyboardSet.CreateTestCaseAddStep,
            AllIcons.Actions.ListFiles,
            new StatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, AUTO_COMPLETE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getStepsSection
    ),

    PRIORITY(
            "Priority",
            KeyboardSet.CreateTestCasePriority,
            AllIcons.Nodes.Favorite,
            new StatusBarItem[]{SAVE, SET_PRIORITY, NAVIGATE_ARROWS},
            true,
            TestCaseUIBase::getPrioritySection
    ),

    SELECT_GROUP(
            "Select / Unselect Group",
            KeyboardSet.SelectGroup,
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    GROUPS(
            "Groups",
            KeyboardSet.CreateTestCaseGroups,
            AllIcons.Nodes.Tag,
            new StatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            TestCaseUIBase::getGroupsSection
    );

    private final String label;
    private final KeyboardSet shortcut;
    private final String customShortcutText;
    private final Icon icon;
    private final StatusBarItem[] statusBarItems;
    private final boolean createMenuItem;
    private final Function<TestCaseUIBase, CreateTestCaseSection> sectionExtractor;

    CreateField(final String label, final KeyboardSet shortcut, final Icon icon, final StatusBarItem[] statusBarItems, final boolean createMenuItem, final Function<TestCaseUIBase, CreateTestCaseSection> sectionExtractor) {
        this.label = label;
        this.shortcut = shortcut;
        this.customShortcutText = null;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.createMenuItem = createMenuItem;
        this.sectionExtractor = sectionExtractor;
    }

    CreateField(final String label, final String customShortcutText, final Icon icon, final StatusBarItem[] statusBarItems, final boolean createMenuItem, final Function<TestCaseUIBase, CreateTestCaseSection> sectionExtractor) {
        this.label = label;
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