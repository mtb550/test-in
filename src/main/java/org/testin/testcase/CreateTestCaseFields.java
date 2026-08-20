package org.testin.testcase;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.statusbar.StatusBarItem;
import org.testin.testcase.create.CreateTestCaseSection;
import org.testin.testcase.create.TestCaseBaseDialog;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.testin.testcase.TestCaseDialogKey.*;

/**
 * A field of the create test case dialog: the section it builds, the icon and
 * placeholder it shows, and the key that jumps to it.
 * <p>
 * Every constant here is a field. The keys a section advertises are
 * {@link TestCaseDialogKey}, which is why this enum no longer carries constants
 * that are null for everything except a name and a keystroke.
 */
@Getter
public enum CreateTestCaseFields implements StatusBarItem {

    DESCRIPTION(
            "Description",
            Shortcuts.CreateTestCaseDescription,
            AllIcons.Actions.Edit,
            TestCaseBaseDialog::getDescriptionSection,
            "set description",
            CORRECTIONS, NAVIGATE_TAB
    ),

    EXPECTED_RESULT(
            "Expected Results",
            Shortcuts.CreateTestCaseExpectedResult,
            AllIcons.General.InspectionsOK,
            TestCaseBaseDialog::getExpectedResultSection,
            "set expected result",
            CORRECTIONS, NAVIGATE_TAB
    ),

    MODULE(
            "Module",
            Shortcuts.CreateTestCaseModule,
            AllIcons.General.ContextHelp,
            TestCaseBaseDialog::getModuleSection,
            "set module",
            CORRECTIONS, NAVIGATE_TAB
    ),

    TEST_DATA(
            "Test Data",
            Shortcuts.EMPTY,
            AllIcons.Nodes.DataTables,
            TestCaseBaseDialog::getTestDataSection,
            "set test data",
            NAVIGATE_TAB
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            Shortcuts.EMPTY,
            AllIcons.Actions.StepOut,
            TestCaseBaseDialog::getPreConditionsSection,
            "set pre conditions",
            CORRECTIONS, NAVIGATE_TAB
    ),

    STEPS(
            "Steps",
            Shortcuts.CreateTestCaseAddStep,
            AllIcons.Actions.ListFiles,
            TestCaseBaseDialog::getStepsSection,
            "set step",
            CORRECTIONS, ADD_STEP, REMOVE_STEP, AUTO_COMPLETE, NAVIGATE_TAB
    ),

    PRIORITY(
            "Priority",
            Shortcuts.CreateTestCasePriority,
            AllIcons.Nodes.Favorite,
            TestCaseBaseDialog::getPrioritySection,
            "",
            SET_PRIORITY, NAVIGATE_ARROWS
    ),

    GROUP(
            "Group",
            Shortcuts.CreateTestCaseGroup,
            AllIcons.Nodes.Tag,
            TestCaseBaseDialog::getGroupSection,
            "",
            NAVIGATE_TAB, SELECT_GROUP
    );

    /**
     * The fields the entry section advertises a jump key for.
     * <p>
     * Held here rather than in DESCRIPTION's own declaration because a constant
     * cannot reference one declared after it — which is the whole reason five
     * duplicate {@code *_SHORTCUT} constants used to exist.
     */
    private static final @NotNull List<CreateTestCaseFields> JUMP_KEYS =
            List.of(DESCRIPTION, EXPECTED_RESULT, STEPS, PRIORITY, GROUP);

    private final @NotNull String name;
    /**
     * Null for a section with no key of its own. Only the sections in
     * {@link #JUMP_KEYS} have their key advertised in the status bar, so a
     * binding outside that list was one nobody could discover.
     */
    /**
     * The key that jumps to this field, and {@link Shortcuts#EMPTY} for the
     * fields no key reaches.
     */
    private final @NotNull Shortcuts shortcut;
    private final @NotNull Icon icon;
    private final @NotNull Function<TestCaseBaseDialog, CreateTestCaseSection> sectionExtractor;

    /**
     * Empty for the two sections with no text field of their own to prompt in.
     */
    private final @NotNull String placeholder;

    /**
     * The keys this section adds to the shared ones.
     */
    private final TestCaseDialogKey @NotNull [] ownKeys;

    CreateTestCaseFields(final @NotNull String name, final @NotNull Shortcuts shortcut, final @NotNull Icon icon,
                         final @NotNull Function<TestCaseBaseDialog, CreateTestCaseSection> sectionExtractor,
                         final @NotNull String placeholder, final TestCaseDialogKey @NotNull ... ownKeys) {
        this.name = name;
        this.shortcut = shortcut;
        this.icon = icon;
        this.sectionExtractor = sectionExtractor;
        this.placeholder = placeholder;
        this.ownKeys = ownKeys;
    }

    /**
     * What the status bar shows while this section holds the focus: the two keys
     * every section shares, then its own, and on the entry section the keys that
     * jump to the other fields.
     */
    public StatusBarItem @NotNull [] getStatusBarItems() {
        final List<StatusBarItem> items = new ArrayList<>();
        items.add(SAVE);
        items.add(CANCEL);
        items.addAll(List.of(ownKeys));

        // Description is where the dialog opens, so its bar is also the map.
        if (this == DESCRIPTION) items.addAll(JUMP_KEYS);

        return items.toArray(StatusBarItem[]::new);
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }
}
