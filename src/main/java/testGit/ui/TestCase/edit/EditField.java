package testGit.ui.TestCase.edit;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.TestCase.edit.bulk.*;
import testGit.util.KeyboardSet;
import testGit.util.statusBar.StatusBarItem;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

@Getter
public enum EditField implements StatusBarItem {
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
            KeyboardSet.UpdateTestCaseTitle,
            AllIcons.Actions.Edit,
            new StatusBarItem[]{SAVE},
            true,
            (items, updatedItems) -> new TitleBulkEditor().show(items, updatedItems)
    ),

    EXPECTED(
            "Expected Results",
            KeyboardSet.UpdateTestCaseExpected,
            AllIcons.General.InspectionsOK,
            new StatusBarItem[]{SAVE},
            true,
            (items, updatedItems) -> new ExpectedBulkEditor().show(items, updatedItems)
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            KeyboardSet.AutoComplete.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    STEPS(
            "Steps",
            KeyboardSet.UpdateTestCaseSteps,
            AllIcons.Actions.ListFiles,
            new StatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, NAVIGATE_TAB, AUTO_COMPLETE},
            true,
            (items, updatedItems) -> new StepsBulkEditor().show(items, updatedItems)
    ),

    SET_PRIORITY(
            "Set Priority",
            KeyboardSet.PriorityHigh.getShortcutText() + " / " + KeyboardSet.PriorityMedium.getShortcutText() + " / " + KeyboardSet.PriorityLow.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null
    ),

    PRIORITY(
            "Priority",
            KeyboardSet.UpdateTestCasePriority,
            AllIcons.Nodes.Favorite,
            new StatusBarItem[]{SAVE, NAVIGATE_ARROWS, SET_PRIORITY},
            true,
            (items, updatedItems) -> new PriorityBulkEditor().show(items, updatedItems)
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
            KeyboardSet.UpdateTestCaseGroups,
            AllIcons.Nodes.Tag,
            new StatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            (items, updatedItems) -> new GroupsBulkEditor().show(items, updatedItems)
    );

    private final String label;
    private final KeyboardSet shortcut;
    private final String customShortcutText;
    private final Icon icon;
    private final StatusBarItem[] statusBarItems;
    private final boolean editMenuItem;
    private final BulkEditorAction bulkAction;

    EditField(final String label, final KeyboardSet shortcut, final Icon icon, final StatusBarItem[] statusBarItems, final boolean editMenuItem, final BulkEditorAction bulkAction) {
        this.label = label;
        this.shortcut = shortcut;
        this.customShortcutText = null;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.editMenuItem = editMenuItem;
        this.bulkAction = bulkAction;
    }

    EditField(final String label, final String customShortcutText, final Icon icon, final StatusBarItem[] statusBarItems, final boolean editMenuItem, final BulkEditorAction bulkAction) {
        this.label = label;
        this.shortcut = null;
        this.customShortcutText = customShortcutText;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.editMenuItem = editMenuItem;
        this.bulkAction = bulkAction;
    }

    @Override
    public String getShortcutText() {
        if (customShortcutText != null) {
            return customShortcutText;
        }
        return shortcut != null ? shortcut.getShortcutText() : "";
    }

    public interface BulkEditorAction {
        void show(final List<TestCaseDto> items, final Consumer<List<TestCaseDto>> updatedItems);
    }
}