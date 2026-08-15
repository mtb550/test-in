package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.statusbar.StatusBarItem;
import org.testin.testcase.create.CreateTestCaseSection;
import org.testin.testcase.create.TestCaseBaseDialog;
import org.testin.testcase.update.bulk.*;
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
public enum UpdateTestCaseFields implements StatusBarItem {

    DESCRIPTION(
            "Description",
            Shortcuts.UpdateTestCaseDescription,
            AllIcons.Actions.Edit,
            GenType.UPDATE_TEST_CASE_DESCRIPTION,
            (p, items, updatedItems) -> new DescriptionBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getDescriptionSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    EXPECTED_RESULT(
            "Expected Results",
            Shortcuts.UpdateTestCaseExpectedResult,
            AllIcons.General.InspectionsOK,
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            (p, items, updatedItems) -> new ExpectedResultBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getExpectedResultSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    MODULE(
            "Module",
            Shortcuts.UpdateTestCaseModule,
            AllIcons.General.ContextHelp,
            GenType.UPDATE_TEST_CASE_MODULE,
            (p, items, updatedItems) -> new ModuleBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getModuleSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    TEST_DATA(
            "Test Data",
            Shortcuts.UpdateTestCaseTestData,
            AllIcons.Nodes.DataTables,
            GenType.UPDATE_TEST_CASE_TEST_DATA,
            (p, items, updatedItems) -> new TestDataBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getTestDataSection,
            new TestCaseDialogKey[]{}
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            Shortcuts.UpdateTestCasePreConditions,
            AllIcons.Actions.StepOut,
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            (p, items, updatedItems) -> new PreConditionsBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getPreConditionsSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    STEPS(
            "Steps",
            Shortcuts.UpdateTestCaseSteps,
            AllIcons.Actions.ListFiles,
            GenType.UPDATE_TEST_CASE_STEPS,
            (p, items, updatedItems) -> new StepsBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getStepsSection,
            new TestCaseDialogKey[]{CORRECTIONS, ADD_STEP, REMOVE_STEP, NAVIGATE_TAB, AUTO_COMPLETE}
    ),

    PRIORITY(
            "Priority",
            Shortcuts.UpdateTestCasePriority,
            AllIcons.Nodes.Favorite,
            GenType.UPDATE_TEST_CASE_PRIORITY,
            (p, items, updatedItems) -> new PriorityBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getPrioritySection,
            new TestCaseDialogKey[]{NAVIGATE_ARROWS, SET_PRIORITY}
    ),

    GROUP(
            "Group",
            Shortcuts.UpdateTestCaseGroup,
            AllIcons.Nodes.Tag,
            GenType.UPDATE_TEST_CASE_GROUP,
            (p, items, updatedItems) -> new GroupBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getGroupSection,
            new TestCaseDialogKey[]{NAVIGATE_TAB, SELECT_GROUP}
    );

    private final @NotNull String name;
    private final @NotNull Shortcuts shortcut;
    private final @NotNull Icon icon;
    private final @NotNull GenType gt;
    private final @NotNull BulkEditorAction bulkAction;
    private final @NotNull Function<TestCaseBaseDialog, CreateTestCaseSection> sectionExtractor;

    /**
     * The keys this section adds to the shared ones.
     */
    private final TestCaseDialogKey @NotNull [] ownKeys;

    /**
     * What the status bar shows while this section holds the focus: the two keys
     * every section shares, then its own.
     */
    public StatusBarItem @NotNull [] getStatusBarItems() {
        final List<StatusBarItem> items = new ArrayList<>();
        items.add(SAVE);
        items.add(CANCEL);
        items.addAll(List.of(ownKeys));

        return items.toArray(StatusBarItem[]::new);
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
