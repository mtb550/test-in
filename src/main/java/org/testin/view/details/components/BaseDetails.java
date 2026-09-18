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
import com.intellij.util.ui.JBFont;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.FontSync;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public abstract class BaseDetails {

    protected float getValueFontSize() {
        return FontSync.getBaseFontSize();
    }

    /**
     * A link in the value's font.
     * <p>
     * Typed rather than inline: ActionLink also takes a Kotlin function of the
     * same shape, and an untyped lambda matches both. Auto-hide is off, because
     * ActionLink hides itself when disabled, and a link that cannot work is shown
     * gray with its reason (#66, finding 150).
     * <p>
     * Not focusable: {@code Tab} moves between the view panel's tabs, and a link
     * holding the keyboard would take {@code Tab} for itself (#311,
     * Rule-VIEW-PANEL-080). A link is clicked.
     */
    protected @NotNull ActionLink link(final @NotNull String text, final @NotNull ActionListener onClick) {
        final @NotNull ActionLink link = new ActionLink(text, onClick);
        link.setAutoHideOnDisable(false);
        link.setFocusable(false);
        link.setFont(JBFont.label().deriveFont(getValueFontSize()));
        return link;
    }

    public abstract int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow);

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull String valueText, final int row) {
        return LabelValueRow.add(panel, gbc, labelText, valueText, getValueFontSize(), row);
    }

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull JComponent valueComponent, final int row) {
        return LabelValueRow.add(panel, gbc, labelText, valueComponent, getValueFontSize(), row);
    }

    /**
     * Adds a component at its natural size, left-aligned - the shape a badge
     * row or an icon row takes, as against the caption and value rows above
     * them.
     * <p>
     * The insets stay the caller's: the two rows sit at different distances from
     * whatever follows them, and that is the only thing they disagree about.
     */
    protected int addFullWidthRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull JComponent component, final @NotNull Insets insets, final int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = insets;

        panel.add(component, gbc);

        return row + 1;
    }
}
