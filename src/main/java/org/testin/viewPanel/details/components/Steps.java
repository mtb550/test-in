package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Steps extends BaseDetails {
    private static final int MARGIN_BOTTOM_PER_STEP = 8;

    @Override
    public int render(@NotNull final Project project, @NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int row) {

        final List<String> steps = dto.getSteps();

        if (steps.isEmpty() || steps.stream().allMatch(String::isBlank))
            return row;

        final JPanel stepsContainer = new JPanel();
        stepsContainer.setLayout(new BoxLayout(stepsContainer, BoxLayout.Y_AXIS));
        stepsContainer.setOpaque(false);

        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).isBlank()) continue;

            final String stepText = (i + 1) + "- " + Services.getInstance(project, Tools.class).format(steps.get(i));
            final int marginBottom = (i == steps.size() - 1) ? 0 : MARGIN_BOTTOM_PER_STEP;
            stepsContainer.add(createStepComponent(stepText, marginBottom));
        }

        return addRow(panel, gbc, TestEditorAttributes.STEPS.getName2(), stepsContainer, row);
    }

    private JTextArea createStepComponent(final String text, final int marginBottom) {
        final JTextArea stepArea = new JTextArea(text);
        stepArea.setFont(JBFont.label().deriveFont(Font.PLAIN, getValueFontSize()));
        stepArea.setLineWrap(true);
        stepArea.setWrapStyleWord(true);
        stepArea.setOpaque(false);
        stepArea.setEditable(false);
        stepArea.setBorder(JBUI.Borders.emptyBottom(marginBottom));
        return stepArea;
    }
}