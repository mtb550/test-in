package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorType;
import org.testin.statusBar.IStatusBarItem;
import org.testin.testCase.createDialog.ICreateTestCaseSection;
import org.testin.testCase.createDialog.TestCaseBaseDialog;
import org.testin.testCase.updateDialog.bulk.*;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.testin.enums.TestCaseDialogKey.*;

/**
 * A field the update menu offers: the section it opens, the bulk editor behind
 * it, and the generator that follows the change into the Java code.
 * <p>
 * Every constant here is a field. The keys a section advertises are
 * {@link TestCaseDialogKey}, shared with {@link CreateTestCaseFields} — the two
 * dialogs offer the same keys, and did so as two separate sets of constants
 * until the keys became a type of their own.
 */
@Getter
@AllArgsConstructor
public enum UpdateTestCaseFields implements IStatusBarItem {

    DESCRIPTION(
            "Description",
            Shortcuts.UpdateTestCaseDescription,
            AllIcons.Actions.Edit,
            GeneratorType.UPDATE_TEST_CASE_DESCRIPTION,
            (p, items, updatedItems) -> new DescriptionBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getDescriptionSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    EXPECTED_RESULT(
            "Expected Results",
            Shortcuts.UpdateTestCaseExpectedResult,
            AllIcons.General.InspectionsOK,
            GeneratorType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            (p, items, updatedItems) -> new ExpectedResultBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getExpectedResultSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    MODULE(
            "Module",
            Shortcuts.UpdateTestCaseModule,
            AllIcons.General.ContextHelp,
            GeneratorType.UPDATE_TEST_CASE_MODULE,
            (p, items, updatedItems) -> new ModuleBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getModuleSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    TEST_DATA(
            "Test Data",
            Shortcuts.UpdateTestCaseTestData,
            AllIcons.Nodes.DataTables,
            GeneratorType.UPDATE_TEST_CASE_TEST_DATA,
            (p, items, updatedItems) -> new TestDataBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getTestDataSection,
            new TestCaseDialogKey[]{}
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            Shortcuts.UpdateTestCasePreConditions,
            AllIcons.Actions.StepOut,
            GeneratorType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            (p, items, updatedItems) -> new PreConditionsBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getPreConditionsSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    STEPS(
            "Steps",
            Shortcuts.UpdateTestCaseSteps,
            AllIcons.Actions.ListFiles,
            GeneratorType.UPDATE_TEST_CASE_STEPS,
            (p, items, updatedItems) -> new StepsBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getStepsSection,
            new TestCaseDialogKey[]{CORRECTIONS, ADD_STEP, REMOVE_STEP, NAVIGATE_TAB, AUTO_COMPLETE}
    ),

    PRIORITY(
            "Priority",
            Shortcuts.UpdateTestCasePriority,
            AllIcons.Nodes.Favorite,
            GeneratorType.UPDATE_TEST_CASE_PRIORITY,
            (p, items, updatedItems) -> new PriorityBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getPrioritySection,
            new TestCaseDialogKey[]{NAVIGATE_ARROWS, SET_PRIORITY}
    ),

    GROUP(
            "Group",
            Shortcuts.UpdateTestCaseGroup,
            AllIcons.Nodes.Tag,
            GeneratorType.UPDATE_TEST_CASE_GROUP,
            (p, items, updatedItems) -> new GroupBulkSectionDialog(p).show(items, updatedItems),
            TestCaseBaseDialog::getGroupSection,
            new TestCaseDialogKey[]{NAVIGATE_TAB, SELECT_GROUP}
    );

    private final @NotNull String name;
    private final @NotNull Shortcuts shortcut;
    private final @NotNull Icon icon;
    private final @NotNull GeneratorType gt;
    private final @NotNull IBulkEditorAction bulkAction;
    private final @NotNull Function<TestCaseBaseDialog, ICreateTestCaseSection> sectionExtractor;

    /**
     * The keys this section adds to the shared ones.
     */
    private final TestCaseDialogKey @NotNull [] ownKeys;

    /**
     * What the status bar shows while this section holds the focus: the two keys
     * every section shares, then its own.
     */
    public IStatusBarItem @NotNull [] getStatusBarItems() {
        final List<IStatusBarItem> items = new ArrayList<>();
        items.add(SAVE);
        items.add(CANCEL);
        items.addAll(List.of(ownKeys));

        return items.toArray(IStatusBarItem[]::new);
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }

    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onTrigger.run();
            }
        }.registerCustomShortcutSet(shortcut.getCustomShortcut(), component);
    }
}
