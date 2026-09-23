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
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.List;
import java.util.function.Consumer;

public class CreateTestCaseDialog extends TestCaseBaseDialog {
    private final @NotNull TestSetDirectoryDto dir;

    // UC-EDITOR-PANEL-005, Rule-CODEGEN-001
    public CreateTestCaseDialog(final @NotNull Project p, final @NotNull TestSetDirectoryDto dir, final @NotNull Consumer<@NotNull TestCaseDto> onSave) {
        super(p, new TestCaseDto(), onSave);
        this.dir = dir;

        descriptionSection.compareAgainst(() -> Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(dir.getPath()));

        final @NotNull TestCaseForm form = new TestCaseForm(descriptionSection::getFocusComponent, true);
        final @NotNull JComponent keys = form.getPanel();

        initDynamicStatusBar(keys);
        showSectionKeys(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());

        for (final CreateTestCaseSection section : getAllSections()) {
            final @NotNull JBPanel<?> slot = form.newSlot();

            section.setupShortcut(keys, slot, this, this::refit);

            if (section instanceof DescriptionSection) {
                section.showSection(slot);
            }
        }

        title = GenType.CREATE_TEST_CASE.getDescription();
        components = List.of(ComponentDialogBase.of(form));
        resizable = true;

        expectedResultSection.enableMultiLine(this, this::submit);
        testDataSection.enableMultiLine(this, this::submit);

        registerShortcut(keys, Shortcuts.Enter.getCustomShortcut(), this::submit);

        registerShortcut(keys, Shortcuts.Escape.getCustomShortcut(), this::closeCancel);
    }

    // UC-EDITOR-PANEL-005
    @Override
    protected void submit() {
        if (!Services.getInstance(p, ProjectIndexer.class).nodeExists(dir.getPath())) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("create.test.case.set.gone", dir.getName()));
            return;
        }

        super.submit();
    }
}
