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
import org.testin.model.BugSeverity;
import org.testin.model.TestRunItems;
import org.testin.testrun.RunEditorAttributes;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.RadioSelection;

public final class BugSeveritySection implements FailureSection {
    @Getter
    private final @NotNull ComponentDialogBase<RadioSelection<BugSeverity>> component;

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-147
    public BugSeveritySection(final @NotNull TestRunItems runItem) {
        component = ComponentDialogBase.<BugSeverity>radios(RunEditorAttributes.BUG_SEVERITY.getName())
                .options(BugSeverity.CHOICES, BugSeverity::getLabel)
                .select(BugSeverity.orDefault(runItem.getBugSeverity()))
                .build();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145
    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        runItem.setBugSeverity(component.getComponent().getSelected());
    }
}
