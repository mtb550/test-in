package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import java.awt.*;

public class Module extends BaseDetails {

    @Override
    public int render(@NotNull final Project project, @NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, TestEditorAttributes.MODULE.getName2(), Services.getInstance(project, Tools.class).format(dto.getModule()), currentRow);
    }
}