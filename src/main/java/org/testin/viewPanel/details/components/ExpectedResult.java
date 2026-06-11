package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import java.awt.*;

public class ExpectedResult extends BaseDetails {
    @Override
    public int render(final @NotNull Project project, @NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int currentRow) {
        return addRow(panel, gbc, TestEditorAttributes.EXPECTED_RESULT.getName2(), Services.getInstance(project, Tools.class).format(dto.getExpectedResult()), currentRow);
    }
}