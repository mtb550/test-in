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
    public int render(@NotNull final Project project, @NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, TestEditorAttributes.CREATE_AT.getName2(), dto.getCreatedAt().format(Config.getDateFormatterPattern()), currentRow);
    }
}