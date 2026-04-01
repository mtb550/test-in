package testGit.viewPanel.details.components;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Steps extends BaseDetails {

    private static final String LABEL_TEXT = "Steps:";
    private static final String EMPTY_PLACEHOLDER = "-";
    private static final int MINIMUM_VISIBLE_STEPS = 7;
    private static final int MARGIN_BOTTOM_PER_STEP = 8;
    private static final int ESTIMATED_STEP_HEIGHT = 35;

    @Override
    public int render(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int row) {

        final JPanel stepsContainer = new JPanel();
        stepsContainer.setLayout(new BoxLayout(stepsContainer, BoxLayout.Y_AXIS));
        stepsContainer.setOpaque(false);

        final List<String> steps = dto.getSteps();

        if (steps == null || steps.isEmpty()) {
            stepsContainer.add(createStepComponent(EMPTY_PLACEHOLDER, 0));
        } else {
            for (int i = 0; i < steps.size(); i++) {
                final String stepText = (i + 1) + "- " + steps.get(i);
                final int marginBottom = (i == steps.size() - 1) ? 0 : MARGIN_BOTTOM_PER_STEP;
                stepsContainer.add(createStepComponent(stepText, marginBottom));
            }
        }

        final int currentCount = (steps == null || steps.isEmpty()) ? 1 : steps.size();
        if (currentCount < MINIMUM_VISIBLE_STEPS) {
            final int missingSteps = MINIMUM_VISIBLE_STEPS - currentCount;
            stepsContainer.add(Box.createVerticalStrut(JBUI.scale(missingSteps * ESTIMATED_STEP_HEIGHT)));
        }

        return addRow(panel, gbc, LABEL_TEXT, stepsContainer, row);
    }

    private JTextArea createStepComponent(final String text, final int marginBottom) {
        final JTextArea stepArea = new JTextArea(text);
        stepArea.setFont(JBFont.label().deriveFont(Font.PLAIN, VALUE_FONT_SIZE));
        stepArea.setLineWrap(true);
        stepArea.setWrapStyleWord(true);
        stepArea.setOpaque(false);
        stepArea.setEditable(false);
        stepArea.setBorder(JBUI.Borders.emptyBottom(marginBottom));
        return stepArea;
    }
}