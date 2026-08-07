package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.CodeGeneratorDialog;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.testCase.createDialog.ICreateTestCaseSection;
import org.testin.testCase.createDialog.TestCaseBaseDialog;
import org.testin.testCase.updateDialog.bulk.*;
import org.testin.util.KeyboardSet;
import org.testin.util.statusBar.IStatusBarItem;

import javax.swing.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public enum UpdateTestCaseFields implements IStatusBarItem {
    SAVE(
            "Save",
            KeyboardSet.Enter,
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
            new IStatusBarItem[]{},
            false,
            null,
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
            null,
            null
    ),

    DESCRIPTION(
            "Description",
            KeyboardSet.UpdateTestCaseDescription,
            AllIcons.Actions.Edit,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_DESCRIPTION,
            (project, items, updatedItems) -> new DescriptionBulkSectionDialog().show(project, items, updatedItems),
            TestCaseBaseDialog::getDescriptionSection
    ),

    EXPECTED_RESULT(
            "Expected Results",
            KeyboardSet.UpdateTestCaseExpectedResult,
            AllIcons.General.InspectionsOK,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            (project, items, updatedItems) -> new ExpectedResultBulkSectionDialog().show(project, items, updatedItems),
            TestCaseBaseDialog::getExpectedResultSection
    ),

    MODULE(
            "Module",
            KeyboardSet.UpdateTestCaseModule,
            AllIcons.General.ContextHelp,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_MODULE,
            (project, items, updatedItems) -> new ModuleBulkSectionDialog().show(project, items, updatedItems),
            TestCaseBaseDialog::getModuleSection
    ),

    TEST_DATA(
            "Test Data",
            KeyboardSet.UpdateTestCaseTestData,
            AllIcons.Nodes.DataTables,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_TEST_DATA,
            (project, items, updatedItems) -> new TestDataBulkSectionDialog().show(project, items, updatedItems),
            TestCaseBaseDialog::getTestDataSection
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            KeyboardSet.UpdateTestCasePreConditions,
            AllIcons.Actions.StepOut,
            new IStatusBarItem[]{SAVE},
            true,
            GeneratorType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            (project, items, updatedItems) -> new PreConditionsBulkSectionDialog().show(project, items, updatedItems),
            TestCaseBaseDialog::getPreConditionsSection
    ),

    AUTO_COMPLETE(
            "Auto Complete",
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
            AllIcons.Actions.ListFiles,
            new IStatusBarItem[]{SAVE, ADD_STEP, REMOVE_STEP, NAVIGATE_TAB, AUTO_COMPLETE},
            true,
            GeneratorType.UPDATE_TEST_CASE_STEPS,
            (project, items, updatedItems) -> new StepsBulkSectionDialog().show(project, items, updatedItems),
            TestCaseBaseDialog::getStepsSection
    ),

    SET_PRIORITY(
            "Set Priority",
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
            AllIcons.Nodes.Favorite,
            new IStatusBarItem[]{SAVE, NAVIGATE_ARROWS, SET_PRIORITY},
            true,
            GeneratorType.UPDATE_TEST_CASE_PRIORITY,
            (project, items, updatedItems) -> new PriorityBulkSectionDialog().show(project, items, updatedItems),
            TestCaseBaseDialog::getPrioritySection
    ),

    SELECT_GROUP(
            "Select / Unselect Group",
            KeyboardSet.SelectGroup,
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
            AllIcons.Nodes.Tag,
            new IStatusBarItem[]{SAVE, NAVIGATE_TAB, SELECT_GROUP},
            true,
            GeneratorType.UPDATE_TEST_CASE_GROUP,
            (project, items, updatedItems) -> new GroupBulkSectionDialog().show(project, items, updatedItems),
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

    UpdateTestCaseFields(final String name, final KeyboardSet shortcut, final Icon icon, final IStatusBarItem[] statusBarItems, final boolean updateMenuItem, final GeneratorType gt, final IBulkEditorAction bulkAction, final Function<TestCaseBaseDialog, ICreateTestCaseSection> sectionExtractor) {
        this.name = name;
        this.shortcut = shortcut;
        this.customShortcutText = null;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.updateMenuItem = updateMenuItem;
        this.gt = gt;
        this.bulkAction = bulkAction;
        this.sectionExtractor = sectionExtractor;
    }

    UpdateTestCaseFields(final String name, final String customShortcutText, final Icon icon, final IStatusBarItem[] statusBarItems, final boolean updateMenuItem, final GeneratorType gt, final IBulkEditorAction bulkAction, final Function<TestCaseBaseDialog, ICreateTestCaseSection> sectionExtractor) {
        this.name = name;
        this.shortcut = null;
        this.customShortcutText = customShortcutText;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.updateMenuItem = updateMenuItem;
        this.gt = gt;
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
            }.registerCustomShortcutSet(this.shortcut.getCustomShortcut(), component);
        }
    }

    public interface IBulkEditorAction {
        void show(final @NotNull Project p, final List<TestCaseDto> items, final BiConsumer<List<TestCaseDto>, CodeGeneratorDialog> updatedItems);
    }
}