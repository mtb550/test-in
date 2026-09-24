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

package org.testin.testrun.failure;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.testrun.RunEditorAttributes;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.TextArea;
import org.testin.util.Bundle;

public final class StacktraceSection implements FailureSection {
    private static final int ROWS = 5;

    @Getter
    private final @NotNull ComponentDialogBase<TextArea> component;

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148
    public StacktraceSection(final @NotNull TestRunItems runItem) {
        component = ComponentDialogBase.textArea()
                .caption(RunEditorAttributes.STACKTRACE.getName())
                .placeholder(Bundle.message("dialog.failure.placeholder.error"))
                .value(runItem.getStacktrace())
                .rows(ROWS)
                .build();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145
    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        runItem.setStacktrace(component.getComponent().getText().trim());
    }
}
