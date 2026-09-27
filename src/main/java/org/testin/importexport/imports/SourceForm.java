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

package org.testin.importexport.imports;

import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.testcase.TestEditorAttributes;
import org.testin.importexport.FileTypes;
import org.testin.importexport.shared.FileDocumentListener;
import org.testin.ui.dialogs.CollapsiblePanel;
import org.testin.ui.dialogs.FormRows;
import org.testin.ui.framework.DialogComponent;

import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class SourceForm implements DialogComponent {
    private static final boolean EXPANDED = true;

    private final @NotNull Project p;
    private final @NotNull SourceSection source;
    private final @NotNull FileTypeHint hint;
    private final @NotNull JComponent panel;

    // UC-SHARE-005, Rule-INTERNAL-099
    public SourceForm(final @NotNull Project p, final @NotNull List<TestEditorAttributes> importAttributes, final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader, final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onDataLoaded) {
        this.p = p;
        source = SourceSection.of(p);
        hint = FileTypeHint.of(importAttributes);

        source.field().getTextField().getDocument().addDocumentListener(
                new FileDocumentListener(source.field(), p, hint::showStatus, (format, parsedData) -> {
                    hint.showFor(format);
                    onDataLoaded.accept(parsedData);
                }, importLoader));

        // UC-SHARE-007, Rule-SHARE-036
        source.field().getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                onDataLoaded.accept(Map.of());
            }
        });

        panel = CollapsiblePanel.build(Bundle.message("import.section"), new FormRows()
                .wideRow(source.panel())
                .wideRow(hint.panel()), EXPANDED);
    }

    // UC-SHARE-005, Rule-SETTING-021
    public void selectSourceFile() {
        source.browse(p);
    }

    // UC-SHARE-005
    public @NotNull Optional<File> resolve() {
        return source.accepted();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return source.field().getTextField();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
