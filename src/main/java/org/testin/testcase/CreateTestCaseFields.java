/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.testcase;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.testcase.create.CreateTestCaseSection;
import org.testin.testcase.create.TestCaseBaseDialog;
import org.testin.util.Bundle;
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
            TestEditorAttributes.DESCRIPTION.getName(),
            Shortcuts.CreateTestCaseDescription,
            AllIcons.Actions.Edit,
            TestCaseBaseDialog::getDescriptionSection,
            Bundle.message("field.set.description"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    EXPECTED_RESULT(
            TestEditorAttributes.EXPECTED_RESULT.getName(),
            Shortcuts.CreateTestCaseExpectedResult,
            AllIcons.General.InspectionsOK,
            TestCaseBaseDialog::getExpectedResultSection,
            Bundle.message("field.set.expected.result"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    MODULE(
            TestEditorAttributes.MODULE.getName(),
            Shortcuts.CreateTestCaseModule,
            AllIcons.General.ContextHelp,
            TestCaseBaseDialog::getModuleSection,
            Bundle.message("field.set.module"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    TEST_DATA(
            TestEditorAttributes.TEST_DATA.getName(),
            Shortcuts.CreateTestCaseTestData,
            AllIcons.Nodes.DataTables,
            TestCaseBaseDialog::getTestDataSection,
            Bundle.message("field.set.test.data"),
            NAVIGATE_TAB
    ),

    PRE_CONDITIONS(
            TestEditorAttributes.PRE_CONDITIONS.getName(),
            Shortcuts.CreateTestCasePreConditions,
            AllIcons.Actions.StepOut,
            TestCaseBaseDialog::getPreConditionsSection,
            Bundle.message("field.set.pre.conditions"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    STEPS(
            TestEditorAttributes.STEPS.getName(),
            Shortcuts.CreateTestCaseAddStep,
            AllIcons.Actions.ListFiles,
            TestCaseBaseDialog::getStepsSection,
            Bundle.message("field.set.step"),
            CORRECTIONS, ADD_STEP, AUTO_COMPLETE, NAVIGATE_TAB
    ),

    PRIORITY(
            TestEditorAttributes.PRIORITY.getName(),
            Shortcuts.CreateTestCasePriority,
            AllIcons.Nodes.Favorite,
            TestCaseBaseDialog::getPrioritySection,
            "",
            NAVIGATE_ARROWS
    ),

    GROUP(
            TestEditorAttributes.GROUP.getName(),
            Shortcuts.CreateTestCaseGroup,
            AllIcons.Nodes.Tag,
            TestCaseBaseDialog::getGroupSection,
            "",
            ADD_GROUP, AUTO_COMPLETE, NAVIGATE_TAB
    );

    /**
     * The fields the entry section advertises a jump key for.
     * <p>
     * Held here rather than in DESCRIPTION's own declaration because a constant
     * cannot reference one declared after it — which is the whole reason five
     * duplicate {@code *_SHORTCUT} constants used to exist.
     */
    private static final @NotNull List<CreateTestCaseFields> JUMP_KEYS =
            List.of(DESCRIPTION, EXPECTED_RESULT, STEPS, TEST_DATA, PRE_CONDITIONS, PRIORITY, GROUP);

    private final @NotNull String name;
    /**
     * The key that jumps to this field, and {@link Shortcuts#EMPTY} for the
     * fields no key reaches.
     * <p>
     * Only the fields in {@link #JUMP_KEYS} have their key advertised in the
     * status bar, so a binding outside that list is one nobody could discover.
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

    CreateTestCaseFields(final @NotNull String name, final @NotNull Shortcuts shortcut, final @NotNull Icon icon, final @NotNull Function<TestCaseBaseDialog, CreateTestCaseSection> sectionExtractor, final @NotNull String placeholder, final TestCaseDialogKey @NotNull ... ownKeys) {
        this.name = name;
        this.shortcut = shortcut;
        this.icon = icon;
        this.sectionExtractor = sectionExtractor;
        this.placeholder = placeholder;
        this.ownKeys = ownKeys;
    }

    /**
     * UC-EDITOR-PANEL-005.
     * <p>
     * What the section strip shows while this section holds the focus: its own
     * keys, and on the entry section the keys that jump to the other fields.
     * <p>
     * Save and Cancel are not here. They mean the same thing in every section,
     * so they sit on a strip of their own that never redraws - see
     * {@link org.testin.testcase.create.StatusBarSection} (#56).
     */
    public StatusBarItem @NotNull [] getStatusBarItems() {
        final @NotNull List<StatusBarItem> items = new ArrayList<>(List.of(ownKeys));

        // Description is where the dialog opens, so its bar is also the map.
        if (this == DESCRIPTION) items.addAll(JUMP_KEYS);

        return items.toArray(StatusBarItem[]::new);
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }
}
