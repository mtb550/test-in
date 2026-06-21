package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;

import java.awt.*;

public class CreatedAt extends BaseDetails {

    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, TestEditorAttributes.CREATE_AT.getName2(), dto.getCreatedAt().format(Config.getDateFormatterPattern()), currentRow);
    }
}