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

import org.testin.codegen.GenType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.notifications.Notifier;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class CreateTestCaseDialog extends TestCaseBaseDialog {

    /**
     * The test set the case is being written into, asked again at every Enter:
     * the dialog is not modal, so the set can go while it is open.
     */
    private final @NotNull TestSetDirectoryDto dir;

    // UC-EDITOR-PANEL-005, Rule-CODEGEN-001
    public CreateTestCaseDialog(final @NotNull Project p, final @NotNull TestSetDirectoryDto dir, final @NotNull Consumer<@NotNull TestCaseDto> onSave) {
        super(p, new TestCaseDto(), onSave);
        this.dir = dir;

        // Asked of the indexer, which owns the test set's cases, at each Enter:
        // the popup is not modal, so the set can change while it is open.
        descriptionSection.compareAgainst(() -> Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(dir.getPath()));

        final @NotNull TestCaseForm form = new TestCaseForm(descriptionSection.getFocusComponent(), true);
        final @NotNull JComponent keys = form.getPanel();

        initDynamicStatusBar(keys);
        showSectionKeys(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());

        // Every section has a slot, and opens on its own key; only the
        // description is open from the start.
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

    /**
     * UC-EDITOR-PANEL-005.
     * <p>
     * Saves only into a test set that is still there. A sync, a pull or the tree
     * can remove or rename the set while the dialog is open, and the case was
     * then written into a folder that was gone (#66, finding 288). Refused with
     * the dialog left open, so what the tester typed is still in front of them.
     */
    @Override
    protected void submit() {
        if (!Services.getInstance(p, ProjectIndexer.class).nodeExists(dir.getPath())) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("create.test.case.set.gone", dir.getName()));
            return;
        }

        super.submit();
    }
}
