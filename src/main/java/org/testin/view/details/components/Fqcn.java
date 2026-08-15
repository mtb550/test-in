package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.awt.*;
import java.util.ArrayList;

public class Fqcn extends BaseDetails {

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final ArrayList<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnMethod(dto);
        return addRow(panel, gbc, TestEditorAttributes.FQCN.getName2(), fqcn.toString(), currentRow);
    }
}