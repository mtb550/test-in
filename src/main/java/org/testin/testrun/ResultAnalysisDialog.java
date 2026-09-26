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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunSummary;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextArea;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ResultAnalysisDialog extends AbstractFrameworkDialog {
    private final @NotNull Map<ResultAnalysis, TextArea> written = new EnumMap<>(ResultAnalysis.class);
    private final @NotNull Consumer<@NotNull Map<ResultAnalysis, String>> onSave;

    // UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-190
    public ResultAnalysisDialog(final @NotNull Project p, final @NotNull TestRunSummary summary, final @NotNull Map<ResultAnalysis, String> current, final @NotNull Consumer<@NotNull Map<ResultAnalysis, String>> onSave) {
        super(p);
        this.onSave = onSave;

        title = Bundle.message("dialog.analysis.title");

        final @NotNull List<ComponentDialogBase<?>> parts = new ArrayList<>();

        for (final ResultAnalysis section : ResultAnalysis.values()) {
            final @NotNull ComponentDialogBase<TextArea> area = ComponentDialogBase.textArea()
                    .placeholder(Bundle.message("dialog.analysis.placeholder", section.getLabel().toLowerCase()))
                    .value(section.writtenIn(current))
                    .rows(3)
                    .build();

            written.put(section, area.getComponent());

            parts.add(ComponentDialogBase.message(section.heading(summary)));
            parts.add(area);
        }

        parts.add(ComponentDialogBase.button(StatusBarShortcut.SAVE));

        components = List.copyOf(parts);

        shortcuts = List.of(
                StatusBarShortcut.navigate(),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.TALL;
    }

    // UC-EDITOR-PANEL-045
    @Override
    protected void submit() {
        final @NotNull Map<ResultAnalysis, String> analysis = new EnumMap<>(ResultAnalysis.class);

        written.forEach((section, area) -> analysis.put(section, area.getText().trim()));

        onSave.accept(analysis);
        closeOk();
    }
}
