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

import org.jetbrains.annotations.NotNull;
import org.testin.model.BugPriority;
import org.testin.model.TestRunItems;
import org.testin.testrun.RunEditorAttributes;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.RadioSelection;

public record BugPrioritySection(@NotNull ComponentDialogBase<RadioSelection<BugPriority>> component) implements FailureSection {
    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-148
    public static @NotNull BugPrioritySection of(final @NotNull TestRunItems runItem) {
        return new BugPrioritySection(ComponentDialogBase.<BugPriority>radios(RunEditorAttributes.BUG_PRIORITY.getName())
                .options(BugPriority.CHOICES, BugPriority::getLabel)
                .select(BugPriority.orDefault(runItem.getBugPriority()))
                .build());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145
    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        runItem.setBugPriority(component.getComponent().getSelected());
    }
}
