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

package org.testin.testcase.create;


import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.*;

public class ExpectedResultSection extends AbstractMultiLineSection {

    public ExpectedResultSection(final @NotNull Project p) {
        super(p, SpellChecker.createCompletionField(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseValues.class).getExpectedResults(), CreateTestCaseFields.EXPECTED_RESULT.getIcon()), ""), CreateTestCaseFields.EXPECTED_RESULT);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-032
    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setExpectedResult(field.getText().trim());
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseExpectedResult.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        field.setText(dto.getExpectedResult());
    }
}
