package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.FontSync;

import javax.swing.*;
import java.awt.*;

public abstract class BaseDetails {

    protected float getLabelFontSize() {
        return FontSync.getBaseFontSize();
    }

    protected float getValueFontSize() {
        return FontSync.getBaseFontSize();
    }

    public abstract int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow);

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull String valueText, final int row) {
        return LabelValueRow.add(panel, gbc, labelText, valueText, getLabelFontSize(), getValueFontSize(), row);
    }

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull JComponent valueComponent, final int row) {
        return LabelValueRow.add(panel, gbc, labelText, valueComponent, getLabelFontSize(), row);
    }

    /**
     * Adds a component across both columns at its natural size, left-aligned -
     * the shape a badge row or an icon row takes, as against the label/value
     * rows above them.
     * <p>
     * The insets stay the caller's: the two rows sit at different distances from
     * whatever follows them, and that is the only thing they disagree about.
     */
    protected int addFullWidthRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull JComponent component, final @NotNull Insets insets, final int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = insets;

        panel.add(component, gbc);

        return row + 1;
    }
}
