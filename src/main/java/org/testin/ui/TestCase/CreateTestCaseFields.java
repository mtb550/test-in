package org.testin.ui.TestCase;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import org.testin.util.KeyboardSet;
import org.testin.util.statusBar.IStatusBarItem;

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
            null,
            null
    ),

    ADD_STEP(
            "Add Step",
            KeyboardSet.CreateTestCaseAddStep,
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
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            KeyboardSet.AutoComplete.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    SET_PRIORITY(
            "Set Priority",
            KeyboardSet.PriorityHigh.getShortcutText() + " / " + KeyboardSet.PriorityMedium.getShortcutText() + " / " + KeyboardSet.PriorityLow.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    NAVIGATE_TAB(
            "Navigate",
            KeyboardSet.TabNext.getShortcutText() + " / " + KeyboardSet.TabPrevious.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    NAVIGATE_ARROWS(
            "Navigate Priority",
            KeyboardSet.ArrowUp.getShortcutText() + " / " + KeyboardSet.ArrowDown.getShortcutText(),
            null,
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    DESCRIPTION(
            "Description",
            KeyboardSet.CreateTestCaseTitle,
            AllIcons.Actions.Edit,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getDescriptionSection,
            "set description"
    ),

    EXPECTED_RESULT(
            "Expected Results",
            KeyboardSet.CreateTestCaseExpected,
            AllIcons.General.InspectionsOK,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getExpectedResultSection,
            "set expected result"
    ),

    STEPS(
            "Steps",
            KeyboardSet.CreateTestCaseAddStep,
            AllIcons.Actions.ListFiles,
            new IStatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, AUTO_COMPLETE, NAVIGATE_TAB},
            true,
            TestCaseUIBase::getStepsSection,
            "set step "
    ),

    PRIORITY(
            "Priority",
            KeyboardSet.CreateTestCasePriority,
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
            new IStatusBarItem[]{},
            false,
            null,
            null
    ),

    GROUP(
            "Group",
            KeyboardSet.CreateTestCaseGroup,
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

    ///  todo, remove constructors and use lombok @AllArgsConstructors
    CreateTestCaseFields(final String name, final KeyboardSet shortcut, final Icon icon, final IStatusBarItem[] statusBarItems, final boolean createMenuItem, final Function<TestCaseUIBase, ICreateTestCaseSection> sectionExtractor, final String placeholder) {
        this.name = name;
        this.shortcut = shortcut;
        this.customShortcutText = null;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.createMenuItem = createMenuItem;
        this.sectionExtractor = sectionExtractor;
        this.placeholder = placeholder;
    }

    ///  todo, remove constructors and use lombok @AllArgsConstructors
    CreateTestCaseFields(final String name, final String customShortcutText, final Icon icon, final IStatusBarItem[] statusBarItems, final boolean createMenuItem, final Function<TestCaseUIBase, ICreateTestCaseSection> sectionExtractor, final String placeholder) {
        this.name = name;
        this.shortcut = null;
        this.customShortcutText = customShortcutText;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.createMenuItem = createMenuItem;
        this.sectionExtractor = sectionExtractor;
        this.placeholder = placeholder;
    }

    @Override
    public String getShortcutText() {
        if (customShortcutText != null) {
            return customShortcutText;
        }
        return shortcut != null ? shortcut.getShortcutText() : "";
    }
}