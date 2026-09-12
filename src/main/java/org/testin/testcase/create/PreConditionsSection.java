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
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.SpellChecker;
import org.testin.util.Shortcuts;

/**
 * What has to be true before this test case can be run - one line, spell
 * checked, and nothing to complete from: a pre-condition is written rather than
 * chosen.
 */
public class PreConditionsSection extends AbstractOneLineSection {

    public PreConditionsSection(final @NotNull Project p) {
        super(SpellChecker.createField(p), CreateTestCaseFields.PRE_CONDITIONS, Shortcuts.CreateTestCasePreConditions);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-032
    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setPreConditions(field.getText().trim());
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        field.setText(dto.getPreConditions());
    }
}
