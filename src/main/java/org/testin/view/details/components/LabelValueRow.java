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

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.framework.Prose;

import javax.swing.*;
import java.awt.*;

/**
 * A caption and the value it names, as the details panel draws every field:
 * the caption on a line of its own, and the value under it across the whole
 * panel.
 * <p>
 * It was a caption column at least 255 pixels wide beside the value, so on a
 * narrow panel the captions took more room than the values: at 350 pixels a
 * value had about 55 to wrap in (#328). With the caption above, neither needs a
 * width of its own, at any panel width or font size.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LabelValueRow {

    private static final int SPACE_ABOVE = 12;
    private static final int CAPTION_GAP = 2;
    private static final int SIDE = 16;

    /**
     * Rule-VIEW-PANEL-006.
     * <p>
     * Adds the value in the read-only wrapping area both callers use. A blank
     * value adds nothing - neither caption nor value - and leaves the row number
     * where it was.
     */
    public static int add(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String caption, final @NotNull String valueText, final float fontSize, final int row) {

        if (valueText.trim().isEmpty()) return row;

        final @NotNull JTextArea valueArea = Prose.of(valueText);
        valueArea.setFont(JBFont.label().deriveFont(Font.PLAIN, fontSize));

        return add(panel, gbc, caption, valueArea, fontSize, row);
    }

    /**
     * UC-VIEW-PANEL-004, Rule-VIEW-PANEL-082.
     * <p>
     * The caption on {@code row} and the value on the row under it, both across
     * the panel, so the next field starts two rows down.
     * <p>
     * {@code REMAINDER} rather than a second column: the header rows above span
     * two, and a field that took one column would leave the other empty beside
     * its value.
     */
    public static int add(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String caption, final @NotNull JComponent value, final float fontSize, final int row) {
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.gridy = row;
        gbc.insets = JBUI.insets(SPACE_ABOVE, SIDE, CAPTION_GAP, SIDE);
        panel.add(Caption.of(caption, fontSize), gbc);

        gbc.gridy = row + 1;
        gbc.insets = JBUI.insets(0, SIDE, 0, SIDE);
        panel.add(value, gbc);

        return row + 2;
    }
}
