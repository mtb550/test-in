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

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class UpdateTestCaseDialog extends TestCaseBaseDialog {

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-036
    public UpdateTestCaseDialog(final @NotNull Project p, final @NotNull TestCaseDto existingDto, final @NotNull UpdateTestCaseFields selectedItem, final @NotNull Consumer<@NotNull TestCaseDto> onSave) {
        super(p, existingDto, onSave);

        // Rule-CODEGEN-001. The same refusal the create dialog makes, which is
        // where it stopped: a description could not be typed into a clash but
        // could be edited into one, and editing is how a clash is likelier to
        // happen - "Verify login" is written first and "Verify login!" is a
        // correction to it (#244).
        //
        // Every case in the set except this one. Comparing against itself would
        // refuse a description the tester did not change.
        descriptionSection.compareAgainst(() -> Services.getInstance(p, ProjectIndexer.class)
                .getTestCasesForTestSet(existingDto.getParent().getPath()).stream()
                .filter(sibling -> !sibling.getId().equals(existingDto.getId()))
                .toList());

        final @NotNull CreateTestCaseSection targetSection = selectedItem.getSectionExtractor().apply(this);

        final @NotNull TestCaseForm form = new TestCaseForm(targetSection.getFocusComponent(), false);
        final @NotNull JComponent keys = form.getPanel();

        // Said once, before the loop: it grays the others out and records
        // that only this one may write back.
        onlyEditable(targetSection);

        for (final CreateTestCaseSection section : getAllSections()) {
            section.fillData(existingDto, this::refit);

            final boolean isTarget = (section == targetSection);

            // Steps and groups are both several boxes, and opening either on a
            // case that has none has to put one there to type into (#296).
            if (isTarget && section instanceof AbstractMultiValueSection s) {
                if (s.getFields().isEmpty()) {
                    s.addField("", this::refit);
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
        components = List.of(ComponentDialogBase.of(form));

        // The expected-result and test-data fields are multi-line text areas:
        // each rebinds Enter, Ctrl+Enter and Tab on itself, since a multi-line
        // editor would otherwise swallow them.
        expectedResultSection.enableMultiLine(this, this::submit);
        testDataSection.enableMultiLine(this, this::submit);

        registerShortcut(keys, Shortcuts.Enter.getCustomShortcut(), this::submit);

        // Escape is bound rather than left to the popup's own cancel key: once an
        // editor popup has been open over the dialog - the spelling corrections,
        // for one - the built-in handler stops seeing the key.
        registerShortcut(keys, Shortcuts.Escape.getCustomShortcut(), this::closeCancel);
    }
}
