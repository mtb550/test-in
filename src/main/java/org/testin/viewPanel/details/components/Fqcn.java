package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import java.awt.*;
import java.util.ArrayList;


// todo, to be removed, not important (commented-001)
public class Fqcn extends BaseDetails {

    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        ArrayList<String> fqcn = Services.getInstance(project, Tools.class).buildFqcnMethod(dto);
        return addRow(panel, gbc, TestEditorAttributes.FQCN.getName2(), fqcn.toString(), currentRow);
    }
}