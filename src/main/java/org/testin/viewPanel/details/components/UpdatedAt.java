package org.testin.viewPanel.details.components;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;

import java.awt.*;

public class UpdatedAt extends BaseDetails {

    private static final String LABEL_TEXT = "Updated At:";

    @Override
    public int render(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, LABEL_TEXT, dto.getUpdatedAt().format(Config.getDateFormatterPattern()), currentRow);
    }
}