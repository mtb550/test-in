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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.model.MenuItem;
import org.testin.model.StatusBarItem;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.create.CreateTestCaseSection;
import org.testin.testcase.create.TestCaseBaseDialog;
import org.testin.testcase.update.bulk.DescriptionBulkSectionDialog;
import org.testin.testcase.update.bulk.ExpectedResultBulkSectionDialog;
import org.testin.testcase.update.bulk.GroupBulkSectionDialog;
import org.testin.testcase.update.bulk.ModuleBulkSectionDialog;
import org.testin.testcase.update.bulk.PreConditionsBulkSectionDialog;
import org.testin.testcase.update.bulk.PriorityBulkSectionDialog;
import org.testin.testcase.update.bulk.StatusBulkSectionDialog;
import org.testin.testcase.update.bulk.StepsBulkSectionDialog;
import org.testin.testcase.update.bulk.TestDataBulkSectionDialog;
import org.testin.util.Bundle;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.function.Function;

import static org.testin.testcase.TestCaseDialogKey.ADD_GROUP;
import static org.testin.testcase.TestCaseDialogKey.ADD_STEP;
import static org.testin.testcase.TestCaseDialogKey.AUTO_COMPLETE;
import static org.testin.testcase.TestCaseDialogKey.CORRECTIONS;
import static org.testin.testcase.TestCaseDialogKey.NAVIGATE_ARROWS;
import static org.testin.testcase.TestCaseDialogKey.NAVIGATE_TAB;

@Getter
@AllArgsConstructor
public enum UpdateTestCaseFields implements MenuItem {
    DESCRIPTION(
            TestEditorAttributes.DESCRIPTION.getName(),
            Shortcuts.UpdateTestCaseDescription,
            CreateTestCaseFields.DESCRIPTION.getIcon(),
            GenType.UPDATE_TEST_CASE_DESCRIPTION,
            (p, items, updatedItems) -> new DescriptionBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getDescriptionSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    EXPECTED_RESULT(
            TestEditorAttributes.EXPECTED_RESULT.getName(),
            Shortcuts.UpdateTestCaseExpectedResult,
            CreateTestCaseFields.EXPECTED_RESULT.getIcon(),
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            (p, items, updatedItems) -> new ExpectedResultBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getExpectedResultSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    MODULE(
            TestEditorAttributes.MODULE.getName(),
            Shortcuts.UpdateTestCaseModule,
            CreateTestCaseFields.MODULE.getIcon(),
            GenType.UPDATE_TEST_CASE_MODULE,
            (p, items, updatedItems) -> new ModuleBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getModuleSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    TEST_DATA(
            TestEditorAttributes.TEST_DATA.getName(),
            Shortcuts.UpdateTestCaseTestData,
            CreateTestCaseFields.TEST_DATA.getIcon(),
            GenType.UPDATE_TEST_CASE_TEST_DATA,
            (p, items, updatedItems) -> new TestDataBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getTestDataSection,
            new TestCaseDialogKey[]{}
    ),

    PRE_CONDITIONS(
            TestEditorAttributes.PRE_CONDITIONS.getName(),
            Shortcuts.UpdateTestCasePreConditions,
            CreateTestCaseFields.PRE_CONDITIONS.getIcon(),
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            (p, items, updatedItems) -> new PreConditionsBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getPreConditionsSection,
            new TestCaseDialogKey[]{CORRECTIONS}
    ),

    STEPS(
            TestEditorAttributes.STEPS.getName(),
            Shortcuts.UpdateTestCaseSteps,
            CreateTestCaseFields.STEPS.getIcon(),
            GenType.UPDATE_TEST_CASE_STEPS,
            (p, items, updatedItems) -> new StepsBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getStepsSection,
            new TestCaseDialogKey[]{CORRECTIONS, ADD_STEP, NAVIGATE_TAB, AUTO_COMPLETE}
    ),

    PRIORITY(
            TestEditorAttributes.PRIORITY.getName(),
            Shortcuts.UpdateTestCasePriority,
            CreateTestCaseFields.PRIORITY.getIcon(),
            GenType.UPDATE_TEST_CASE_PRIORITY,
            (p, items, updatedItems) -> new PriorityBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getPrioritySection,
            new TestCaseDialogKey[]{NAVIGATE_ARROWS}
    ),

    GROUP(
            TestEditorAttributes.GROUP.getName(),
            Shortcuts.UpdateTestCaseGroup,
            CreateTestCaseFields.GROUP.getIcon(),
            GenType.UPDATE_TEST_CASE_GROUP,
            (p, items, updatedItems) -> new GroupBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getGroupSection,
            new TestCaseDialogKey[]{ADD_GROUP, AUTO_COMPLETE, NAVIGATE_TAB}
    ),

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    STATUS(
            TestEditorAttributes.STATUS.getName(),
            Shortcuts.EMPTY,
            AllIcons.Actions.Preview,
            GenType.UPDATE_TEST_CASE_STATUS,
            (p, items, updatedItems) -> new StatusBulkSectionDialog(p, items, updatedItems).open(),
            TestCaseBaseDialog::getStatusSection,
            new TestCaseDialogKey[]{}
    ),

    ORDER(
            TestEditorAttributes.ORDER.getName(),
            Shortcuts.UpdateTestCaseOrder,
            Icons.fieldLetter("O", Icons.GRAY),
            GenType.UPDATE_TEST_CASE_ORDER,
            (p, _, _) -> Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("update.order.one.at.a.time")),
            TestCaseBaseDialog::getOrderSection,
            new TestCaseDialogKey[]{}
    );

    private final @NotNull String name;
    private final @NotNull Shortcuts shortcut;
    private final @NotNull Icon icon;
    private final @NotNull GenType gt;
    private final @NotNull BulkEditorAction bulkAction;
    private final @NotNull Function<TestCaseBaseDialog, CreateTestCaseSection> sectionExtractor;

    private final TestCaseDialogKey @NotNull [] ownKeys;

    // Rule-EDITOR-PANEL-199
    public StatusBarItem @NotNull [] getStatusBarItems() {
        return ownKeys.clone();
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }

    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        if (Shortcuts.isNoKey(shortcut.getKey())) return;

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onTrigger.run();
            }
        }.registerCustomShortcutSet(shortcut.getCustomShortcut(), component);
    }
}
