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
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.Caption;
import org.testin.util.Fonts;

import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;

@AllArgsConstructor
public final class BandTitle extends BaseDetails {
    private static final int GAP = 10;
    private static final int INSETS_TOP = 18;
    private static final int INSETS_SIDE = 16;

    private final @NotNull String name;

    // UC-VIEW-PANEL-004, UC-VIEW-PANEL-005, Rule-VIEW-PANEL-085
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull JBPanel<?> band = new JBPanel<>(new BorderLayout(JBUI.scale(GAP), 0));
        band.setOpaque(false);

        band.add(Caption.of(name, Fonts.panelCaption()), BorderLayout.WEST);
        band.add(new JSeparator(), BorderLayout.CENTER);

        return addStretchedRow(panel, gbc, band, JBUI.insets(INSETS_TOP, INSETS_SIDE, 0, INSETS_SIDE), currentRow);
    }
}
