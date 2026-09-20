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

package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunStatus;
import org.testin.report.GenerateReportAction;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

public class GenerateReportBtn extends AbstractIconButton implements ToolbarItem {
    private final @NotNull RunEditor editor;

    // UC-REPORT-001
    public GenerateReportBtn(final @NotNull Project p, final @NotNull RunEditor editor) {
        super(Bundle.message("toolbar.report"), AllIcons.ToolbarDecorator.Export, Shortcuts.GenerateReport);
        this.editor = editor;

        addActionListener(e -> new GenerateReportAction(p, editor).execute());

        updateEnabledState();
    }

    // UC-REPORT-001, Rule-REPORT-016
    public void updateEnabledState() {
        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();

        setEnabled(status.isReportable());
        describe(status.isReportable()
                ? Bundle.message("toolbar.report")
                : Bundle.message("toolbar.report.disabled", status.getLabel()));
    }
}
