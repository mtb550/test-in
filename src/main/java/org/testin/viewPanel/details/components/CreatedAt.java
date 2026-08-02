package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.Config;
import org.testin.mappers.dto.TestCaseDto;

import java.awt.*;

public class CreatedAt extends BaseDetails {

    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, TestEditorAttributes.CREATE_AT.getName2(), dto.getCreatedAt().format(Config.getDateFormatterPattern()), currentRow);
    }
}