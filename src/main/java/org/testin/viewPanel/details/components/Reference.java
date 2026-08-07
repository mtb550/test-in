package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.awt.*;

public class Reference extends BaseDetails {

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, TestEditorAttributes.REFERENCE.getName2(), Services.getInstance(p, Tools.class).format(dto.getReference()), currentRow);
    }
}