package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;

import java.awt.*;
import java.util.function.BiFunction;

/**
 * Generic label/value row for the details panel. Replaces the former one-line
 * component classes (ExpectedResult, PreConditions, TestData, Module, Reference,
 * CreatedBy/At, UpdatedBy/At) with a single attribute + extractor pair.
 */
public final class AttributeRow extends BaseDetails {

    private final @NotNull TestEditorAttributes attribute;
    private final @NotNull BiFunction<Project, TestCaseDto, String> extractor;

    public AttributeRow(final @NotNull TestEditorAttributes attribute,
                        final @NotNull BiFunction<Project, TestCaseDto, String> extractor) {
        this.attribute = attribute;
        this.extractor = extractor;
    }

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, attribute.getName2(), extractor.apply(p, dto), currentRow);
    }
}
