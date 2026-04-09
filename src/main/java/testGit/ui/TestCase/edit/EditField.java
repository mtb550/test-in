package testGit.ui.TestCase.edit;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.TestCase.CreateTestCaseSection;
import testGit.ui.TestCase.TestCaseUIBase;
import testGit.ui.TestCase.edit.bulk.*;
import testGit.util.KeyboardSet;
import testGit.util.statusBar.StatusBarItem;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public enum EditField implements StatusBarItem {
    SAVE(
            "Save",
            KeyboardSet.Enter,
            null,
            new StatusBarItem[]{},
            false,
            null,
            null
    ),

    ADD_STEP(
            "Add Step",
            KeyboardSet.CreateTestCaseAddStep,
            null,
            new StatusBarItem[]{},
            false,
            null,
            null
    ),

    REMOVE_STEP(
            "Remove Step",
            KeyboardSet.CreateTestCaseRemoveStep,
            null,
            new StatusBarItem[]{},
            false,
            null,
            null
    ),

    NAVIGATE_TAB(
            "Navigate",
            KeyboardSet.TabNext.getShortcutText() + " / " + KeyboardSet.TabPrevious.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null,
            null
    ),

    NAVIGATE_ARROWS(
            "Navigate Priority",
            KeyboardSet.ArrowUp.getShortcutText() + " / " + KeyboardSet.ArrowDown.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null,
            null
    ),

    TITLE(
            "Title",
            KeyboardSet.UpdateTestCaseTitle,
            AllIcons.Actions.Edit,
            new StatusBarItem[]{SAVE},
            true,
            (items, updatedItems) -> new TitleBulkEditor().show(items, updatedItems),
            TestCaseUIBase::getTitleSection
    ),

    EXPECTED(
            "Expected Results",
            KeyboardSet.UpdateTestCaseExpected,
            AllIcons.General.InspectionsOK,
            new StatusBarItem[]{SAVE},
            true,
            (items, updatedItems) -> new ExpectedBulkEditor().show(items, updatedItems),
            TestCaseUIBase::getExpectedSection
    ),

    AUTO_COMPLETE(
            "Auto Complete",
            KeyboardSet.AutoComplete.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null,
            null
    ),

    STEPS(
            "Steps",
            KeyboardSet.UpdateTestCaseSteps,
            AllIcons.Actions.ListFiles,
            new StatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, NAVIGATE_TAB, AUTO_COMPLETE},
            true,
            (items, updatedItems) -> new StepsBulkEditor().show(items, updatedItems),
            TestCaseUIBase::getStepsSection
    ),

    SET_PRIORITY(
            "Set Priority",
            KeyboardSet.PriorityHigh.getShortcutText() + " / " + KeyboardSet.PriorityMedium.getShortcutText() + " / " + KeyboardSet.PriorityLow.getShortcutText(),
            null,
            new StatusBarItem[]{},
            false,
            null,
            null
    ),

    PRIORITY(
            "Priority",
            KeyboardSet.UpdateTestCasePriority,
            AllIcons.Nodes.Favorite,
            new StatusBarItem[]{SAVE, NAVIGATE_ARROWS, SET_PRIORITY},
            true,
            (items, updatedItems) -> new PriorityBulkEditor().show(items, updatedItems),
            TestCaseUIBase::getPrioritySection
    ),

    SELECT_GROUP(
            "Select / Unselect Group",
            KeyboardSet.SelectGroup,
            null,
            new StatusBarItem[]{},
            false,
            null,
            null
    ),

    GROUPS(
            "Groups",
            KeyboardSet.UpdateTestCaseGroups,
            AllIcons.Nodes.Tag,
            new StatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            (items, updatedItems) -> new GroupsBulkEditor().show(items, updatedItems),
            TestCaseUIBase::getGroupsSection
    );

    private final String label;
    private final KeyboardSet shortcut;
    private final String customShortcutText;
    private final Icon icon;
    private final StatusBarItem[] statusBarItems;
    private final boolean editMenuItem;
    private final BulkEditorAction bulkAction;
    private final Function<TestCaseUIBase, CreateTestCaseSection> sectionExtractor;

    EditField(final String label, final KeyboardSet shortcut, final Icon icon, final StatusBarItem[] statusBarItems, final boolean editMenuItem, final BulkEditorAction bulkAction, final Function<TestCaseUIBase, CreateTestCaseSection> sectionExtractor) {
        this.label = label;
        this.shortcut = shortcut;
        this.customShortcutText = null;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.editMenuItem = editMenuItem;
        this.bulkAction = bulkAction;
        this.sectionExtractor = sectionExtractor;
    }

    EditField(final String label, final String customShortcutText, final Icon icon, final StatusBarItem[] statusBarItems, final boolean editMenuItem, final BulkEditorAction bulkAction, final Function<TestCaseUIBase, CreateTestCaseSection> sectionExtractor) {
        this.label = label;
        this.shortcut = null;
        this.customShortcutText = customShortcutText;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.editMenuItem = editMenuItem;
        this.bulkAction = bulkAction;
        this.sectionExtractor = sectionExtractor;
    }

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
            }.registerCustomShortcutSet(this.shortcut.getShortcut(), component);
        }
    }

    public interface BulkEditorAction {
        void show(final List<TestCaseDto> items, final Consumer<List<TestCaseDto>> updatedItems);
    }
}