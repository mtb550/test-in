package org.testin.view.details.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * The label/value row drawn by the details panel and by the marker details
 * popup. Both built it identically, down to the label width and the insets, so
 * the two could drift apart while still claiming to be one design.
 * <p>
 * Font sizes are arguments rather than read here, so a caller that overrides
 * them keeps deciding how its own rows look.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LabelValueRow {

    private static final int LABEL_WIDTH = 255;
    private static final int LABEL_INSETS_TOP = 12;
    private static final int LABEL_INSETS_LEFT = 16;
    private static final int LABEL_INSETS_BOTTOM = 12;
    private static final int LABEL_INSETS_RIGHT = 8;
    private static final int VALUE_INSETS_TOP = 12;
    private static final int VALUE_INSETS_LEFT = 0;
    private static final int VALUE_INSETS_BOTTOM = 12;
    private static final int VALUE_INSETS_RIGHT = 16;

    /**
     * Adds the value in the read-only wrapping area both callers use. A blank
     * value adds nothing and leaves the row number where it was.
     */
    public static int add(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc,
                          final @NotNull String labelText, final @NotNull String valueText,
                          final float labelFontSize, final float valueFontSize, final int row) {

        if (valueText.trim().isEmpty()) return row;

        final JTextArea valueArea = new JTextArea(valueText);
        valueArea.setFont(JBFont.label().deriveFont(Font.PLAIN, valueFontSize));
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);
        valueArea.setOpaque(false);
        valueArea.setEditable(false);
        valueArea.setBorder(null);

        return add(panel, gbc, labelText, valueArea, labelFontSize, row);
    }

    public static int add(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc,
                          final @NotNull String labelText, final @NotNull JComponent valueComponent,
                          final float labelFontSize, final int row) {

        gbc.gridy = row;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(LABEL_INSETS_TOP, LABEL_INSETS_LEFT, LABEL_INSETS_BOTTOM, LABEL_INSETS_RIGHT);

        final JBLabel label = new JBLabel(labelText);
        label.setForeground(JBColor.GRAY);
        label.setFont(JBFont.label().deriveFont(Font.BOLD, labelFontSize));

        final Dimension prefSize = label.getPreferredSize();
        label.setPreferredSize(new Dimension(LABEL_WIDTH, prefSize.height));
        label.setMinimumSize(new Dimension(LABEL_WIDTH, prefSize.height));

        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(VALUE_INSETS_TOP, VALUE_INSETS_LEFT, VALUE_INSETS_BOTTOM, VALUE_INSETS_RIGHT);

        panel.add(valueComponent, gbc);

        return row + 1;
    }
}
