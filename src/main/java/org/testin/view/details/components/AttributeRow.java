package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.awt.*;
import java.util.function.BiFunction;

/**
 * Generic label/value row for the details panel. Replaces the former one-line
 * component classes (ExpectedResult, PreConditions, TestData, Module, Reference,
 * CreatedBy/At, UpdatedBy/At) with a single caption + extractor pair.
 * <p>
 * Rule-VIEW-PANEL-061. A caption rather than the attribute it usually comes
 * from, because the two audit rows say who and when together: "Created" is a
 * row this panel makes rather than a field anything stores (#23).
 */
@RequiredArgsConstructor
public final class AttributeRow extends BaseDetails {

    private final @NotNull String caption;
    private final @NotNull BiFunction<Project, TestCaseDto, String> extractor;

    // UC-VIEW-PANEL-004
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, caption, extractor.apply(p, dto), currentRow);
    }
}
