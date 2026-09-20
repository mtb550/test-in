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

import org.testin.editor.run.ExecutionControl;
import org.testin.editor.AbstractIconButton;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunStatus;
import org.testin.util.Bundle;

public class StartExecutionBtn extends AbstractIconButton implements ToolbarItem {
    private final @NotNull RunEditor editor;

    public StartExecutionBtn(final @NotNull RunEditor editor, final @NotNull Runnable onStartExecutionClicked) {
        super(ExecutionControl.START.getLabel(), ExecutionControl.START.getIcon());
        this.editor = editor;

        addActionListener(e -> onStartExecutionClicked.run());
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public static @NotNull String tooltipFor(final @NotNull RunEditor editor) {
        if (editor.isExecuting()) return Bundle.message("toolbar.executing");

        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        if (status.isTerminal()) return Bundle.message("toolbar.execution.disabled", status.getLabel());

        return editor.hasSomethingToWalk()
                ? ExecutionControl.START.getLabel()
                : Bundle.message("toolbar.nothing.to.execute");
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public void updateEnabledState() {
        setEnabled(editor.canStartManualExecution());
        describe(tooltipFor(editor));
    }
}