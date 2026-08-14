package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.TestCaseDto;
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

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @Nullable String valueText, final int row) {
        return LabelValueRow.add(panel, gbc, labelText, valueText, getLabelFontSize(), getValueFontSize(), row);
    }

    protected int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull JComponent valueComponent, final int row) {
        return LabelValueRow.add(panel, gbc, labelText, valueComponent, getLabelFontSize(), row);
    }
}
