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

package org.testin.testcase.update;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.create.AbstractMultiValueSection;
import org.testin.testcase.create.CreateTestCaseSection;
import org.testin.testcase.create.DescriptionSection;
import org.testin.testcase.create.ExpectedResultSection;
import org.testin.testcase.create.TestCaseBaseDialog;
import org.testin.testcase.create.TestCaseForm;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.List;
import java.util.function.Consumer;

public class UpdateTestCaseDialog extends TestCaseBaseDialog {
    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-036
    public UpdateTestCaseDialog(final @NotNull Project p, final @NotNull TestCaseDto existingDto, final @NotNull UpdateTestCaseFields selectedItem, final @NotNull Consumer<@NotNull TestCaseDto> onSave) {
        super(p, existingDto, onSave);

        // Rule-CODEGEN-001
        descriptionSection.compareAgainst(() -> Services.getInstance(p, ProjectIndexer.class)
                .getTestCasesForTestSet(existingDto.getParent().getPath()).stream()
                .filter(sibling -> !sibling.getId().equals(existingDto.getId()))
                .toList());

        final @NotNull CreateTestCaseSection targetSection = selectedItem.getSectionExtractor().apply(this);

        final @NotNull TestCaseForm form = new TestCaseForm(targetSection::getFocusComponent, false);
        final @NotNull JComponent keys = form.getPanel();

        onlyEditable(targetSection);

        for (final CreateTestCaseSection section : getAllSections()) {
            section.fillData(existingDto);

            final boolean isTarget = (section == targetSection);

            if (isTarget && section instanceof AbstractMultiValueSection s) {
                if (s.getFields().isEmpty()) {
                    s.addField("");
                }
            }

            final boolean showAlways = section instanceof DescriptionSection;
            final boolean showIfNotEmpty = section instanceof ExpectedResultSection && !existingDto.getExpectedResult().isEmpty();
            if (!(showAlways || showIfNotEmpty || isTarget)) continue;

            final @NotNull JBPanel<?> slot = form.newSlot();
            section.showSection(slot);

            if (isTarget) {
                section.setupShortcut(keys, slot, this, this::refit);
            }
        }

        showSectionKeys(selectedItem.getStatusBarItems());

        title = Bundle.message("update.dialog.title.field", selectedItem.getName());

        // Rule-INTERNAL-101
        resizable = true;
        components = List.of(ComponentDialogBase.of(form));

        getAllSections().forEach(section -> section.enableMultiLine(this, this::submit));

        registerShortcut(keys, Shortcuts.Enter.getCustomShortcut(), this::submit);

        registerShortcut(keys, Shortcuts.Escape.getCustomShortcut(), this::closeCancel);
    }
}
