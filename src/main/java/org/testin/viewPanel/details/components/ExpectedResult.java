package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.awt.*;

public class ExpectedResult extends BaseDetails {
    @Override
    public int render(final @NotNull Project p, @NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int currentRow) {
        return addRow(panel, gbc, TestEditorAttributes.EXPECTED_RESULT.getName2(), Services.getInstance(p, Tools.class).format(dto.getExpectedResult()), currentRow);
    }
}