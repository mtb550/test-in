package org.testin.ui.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestRunItems;
import org.testin.ui.testRun.update.RunItemEditSection;

import javax.swing.*;
import java.awt.*;

public class ActualResultSection implements RunItemEditSection {

    private static final float FIELD_FONT_SIZE_OFFSET = 4f;
    private static final float LABEL_FONT_SIZE_OFFSET = 2f;

    @Getter
    private final JBTextField actualResultField;

    @Getter
    private final JPanel wrapper;
    private final JBLabel descriptionLabel;
    private final JBLabel expectedResultLabel;

    public ActualResultSection() {
        this.actualResultField = new JBTextField();
        this.actualResultField.setFont(JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + FIELD_FONT_SIZE_OFFSET));
        this.actualResultField.setBorder(JBUI.Borders.empty(10));

        this.descriptionLabel = new JBLabel();
        this.descriptionLabel.setFont(JBFont.regular().deriveFont(Font.BOLD, JBUI.Fonts.label().getSize2D() + LABEL_FONT_SIZE_OFFSET));

        this.expectedResultLabel = new JBLabel();
        this.expectedResultLabel.setFont(JBFont.regular().deriveFont(Font.PLAIN, JBUI.Fonts.label().getSize2D() + LABEL_FONT_SIZE_OFFSET));

        this.wrapper = buildPanel();
    }

    private JPanel buildPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4, 0, 4, 0);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;

        int row = 0;

        // Description row
        gbc.gridy = row++;
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.setOpaque(false);
        descPanel.add(createIconPanel(AllIcons.Actions.Edit), BorderLayout.WEST);
        descPanel.add(descriptionLabel, BorderLayout.CENTER);
        panel.add(descPanel, gbc);

        // Expected result row
        gbc.gridy = row++;
        JPanel expPanel = new JPanel(new BorderLayout());
        expPanel.setOpaque(false);
        expPanel.add(createIconPanel(AllIcons.General.InspectionsOK), BorderLayout.WEST);
        expPanel.add(expectedResultLabel, BorderLayout.CENTER);
        panel.add(expPanel, gbc);

        // Actual result row
        gbc.gridy = row++;
        JPanel actualPanel = new JPanel(new BorderLayout());
        actualPanel.setOpaque(false);
        actualPanel.add(createIconPanel(AllIcons.Actions.Copy), BorderLayout.WEST);
        actualPanel.add(actualResultField, BorderLayout.CENTER);
        panel.add(actualPanel, gbc);

        return panel;
    }

    @Override
    public void fillData(final @NotNull TestRunItems runItem) {
        descriptionLabel.setText("Description: " + runItem.getTc().getDescription());
        expectedResultLabel.setText("Expected Result: " + runItem.getTc().getExpectedResult());
        actualResultField.setText(runItem.getActualResult());
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        runItem.setActualResult(actualResultField.getText().trim());
    }

    @Override
    public void showSection(final JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        actualResultField.requestFocus();
    }

    @Override
    public JComponent getFocusComponent() {
        return actualResultField;
    }

    public JPanel createIconPanel(final Icon icon) {
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}
