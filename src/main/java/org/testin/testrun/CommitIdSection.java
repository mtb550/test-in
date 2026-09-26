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
package org.testin.testrun;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunConfiguration;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.util.Map;

public record CommitIdSection(@NotNull ComponentDialogBase<TextInput> component) implements RunSection {
    // UC-TREE-PANEL-021
    public static @NotNull CommitIdSection of(final @NotNull String value) {
        return new CommitIdSection(ComponentDialogBase.textField()
                .caption(TestRunConfiguration.COMMIT_ID.getDisplayName())
                .placeholder(Bundle.message("run.form.commit.hint"))
                .value(value)
                .build());
    }

    @Override
    public void applyTo(final @NotNull Map<TestRunConfiguration, String> answers) {
        answers.put(TestRunConfiguration.COMMIT_ID, component.getComponent().getText().trim());
    }
}
