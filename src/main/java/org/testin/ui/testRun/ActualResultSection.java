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

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);
    final Font labelFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f);

    @Getter
    private final JBTextField actualResultField;
    private final JPanel wrapper;
    private final JBLabel descriptionLabel;
    private final JBLabel expectedResultLabel;

    public ActualResultSection() {
        this.actualResultField = new JBTextField();
        this.actualResultField.setFont(fieldFont);
        this.actualResultField.setBorder(JBUI.Borders.empty(10));

        this.descriptionLabel = new JBLabel();
        this.descriptionLabel.setFont(labelFont);

        this.expectedResultLabel = new JBLabel();
        this.expectedResultLabel.setFont(labelFont);

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
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        actualResultField.requestFocus();
    }

    @Override
    public void fillData(final @NotNull TestRunItems runItem) {
        descriptionLabel.setText(runItem.getTc().getDescription());
        expectedResultLabel.setText(runItem.getTc().getExpectedResult());
        actualResultField.setText(runItem.getActualResult());
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        if (wrapper.getParent() != null) {
            runItem.setActualResult(actualResultField.getText().trim());
        }
    }

    @Override
    public JComponent getFocusComponent() {
        return actualResultField;
    }

    @Override
    public JPanel createIconPanel(final Icon icon) {
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}
