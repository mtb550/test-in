package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestEditorAttributes;
import org.testin.ui.framework.Prose;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Display;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Steps extends BaseDetails {
    private static final int MARGIN_BOTTOM_PER_STEP = 8;

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int row) {

        final @NotNull List<String> steps = dto.getSteps();

        if (steps.isEmpty() || steps.stream().allMatch(String::isBlank))
            return row;

        final @NotNull JBPanel<?> stepsContainer = new JBPanel<>();
        stepsContainer.setLayout(new BoxLayout(stepsContainer, BoxLayout.Y_AXIS));
        stepsContainer.setOpaque(false);

        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).isBlank()) continue;

            final @NotNull String stepText = Display.numberedStep(i, steps.get(i));
            final int marginBottom = (i == steps.size() - 1) ? 0 : MARGIN_BOTTOM_PER_STEP;
            stepsContainer.add(createStepComponent(stepText, marginBottom));
        }

        return addRow(panel, gbc, TestEditorAttributes.STEPS.getName2(), stepsContainer, row);
    }

    private @NotNull JTextArea createStepComponent(final @NotNull String text, final int marginBottom) {
        final @NotNull JTextArea stepArea = Prose.of(text);
        stepArea.setFont(JBFont.label().deriveFont(Font.PLAIN, getValueFontSize()));

        // The one place prose carries a border: the gap between two steps.
        stepArea.setBorder(JBUI.Borders.emptyBottom(marginBottom));
        return stepArea;
    }
}