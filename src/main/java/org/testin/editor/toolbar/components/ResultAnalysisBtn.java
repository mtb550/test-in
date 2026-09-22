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

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractIconButton;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunStatus;
import org.testin.util.Bundle;

public class ResultAnalysisBtn extends AbstractIconButton implements ToolbarItem {
    private final @NotNull RunEditor editor;

    // UC-EDITOR-PANEL-045
    public ResultAnalysisBtn(final @NotNull RunEditor editor, final @NotNull Runnable onResultAnalysisClicked) {
        super(Bundle.message("toolbar.analysis"), AllIcons.Actions.ProjectWideAnalysisOff);
        this.editor = editor;

        addActionListener(_ -> onResultAnalysisClicked.run());
        updateEnabledState();
    }

    // UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-189
    public void updateEnabledState() {
        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        final boolean completed = status == TestRunStatus.COMPLETED;

        setEnabled(completed);
        describe(completed
                ? Bundle.message("toolbar.analysis")
                : Bundle.message("toolbar.analysis.disabled", status.getLabel()));
    }
}
