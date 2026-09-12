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

package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.awt.*;
import java.util.function.BiFunction;

/**
 * Generic label/value row for the details panel. Replaces the former one-line
 * component classes (ExpectedResult, PreConditions, TestData, Module, Reference,
 * CreatedBy/At, UpdatedBy/At) with a single caption + extractor pair.
 * <p>
 * Rule-VIEW-PANEL-061. A caption rather than the attribute it usually comes
 * from, because the two audit rows say who and when together: "Created" is a
 * row this panel makes rather than a field anything stores (#23).
 */
@RequiredArgsConstructor
public final class AttributeRow extends BaseDetails {

    private final @NotNull String caption;
    private final @NotNull BiFunction<Project, TestCaseDto, String> extractor;

    // UC-VIEW-PANEL-004
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, caption, extractor.apply(p, dto), currentRow);
    }
}
