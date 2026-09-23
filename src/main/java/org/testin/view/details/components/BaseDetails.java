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
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;

public abstract class BaseDetails {
    // Rule-VIEW-PANEL-080
    protected @NotNull ActionLink link(final @NotNull String text, final @NotNull ActionListener onClick) {
        final @NotNull ActionLink link = new ActionLink(text, onClick);
        link.setAutoHideOnDisable(false);
        link.setFocusable(false);
        link.setFont(Fonts.body());
        return link;
    }

    public abstract int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow);

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull String valueText, final int row) {
        return LabelValueRow.add(panel, gbc, labelText, valueText, row);
    }

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull JComponent valueComponent, final int row) {
        return LabelValueRow.add(panel, gbc, labelText, valueComponent, row);
    }

    protected int addFullWidthRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull JComponent component, final @NotNull Insets insets, final int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = insets;

        panel.add(component, gbc);

        return row + 1;
    }

    // Rule-VIEW-PANEL-085
    protected int addStretchedRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull JComponent component, final @NotNull Insets insets, final int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = insets;

        panel.add(component, gbc);

        return row + 1;
    }
}
