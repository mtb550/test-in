package org.testin.testRun.createDialog;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.TestRunItems;
import org.testin.testRun.updateDialog.RunItemEditSection;

import javax.swing.*;
import java.awt.*;

public class ActualResultSection implements RunItemEditSection {

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    @Getter
    private final JBTextField actualResultField;
    private final JBPanel<?> wrapper;
    private final JBLabel descriptionLabel;
    private final JBLabel expectedResultLabel;

    public ActualResultSection() {
        this.actualResultField = new JBTextField();
        this.actualResultField.setFont(fieldFont);
        this.actualResultField.setBorder(JBUI.Borders.empty(10));

        this.descriptionLabel = new JBLabel();
        this.descriptionLabel.setFont(fieldFont);

        this.expectedResultLabel = new JBLabel();
        this.expectedResultLabel.setFont(fieldFont);

        this.wrapper = buildPanel();
    }

    private JBPanel<?> buildPanel() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Description row — same pattern as ExpectedResultSection
        JBPanel<?> descRow = new JBPanel<>(new BorderLayout());
        descRow.setOpaque(false);
        descRow.add(createIconPanel(AllIcons.Actions.Edit), BorderLayout.WEST);
        descRow.add(descriptionLabel, BorderLayout.CENTER);
        descRow.setBorder(JBUI.Borders.emptyTop(8));
        panel.add(descRow);

        // Expected Result row — same pattern as ExpectedResultSection
        JBPanel<?> expRow = new JBPanel<>(new BorderLayout());
        expRow.setOpaque(false);
        expRow.add(createIconPanel(AllIcons.General.InspectionsOK), BorderLayout.WEST);
        expRow.add(expectedResultLabel, BorderLayout.CENTER);
        expRow.setBorder(JBUI.Borders.emptyTop(8));
        panel.add(expRow);

        // Actual Result row — same pattern as ExpectedResultSection
        JBPanel<?> actRow = new JBPanel<>(new BorderLayout());
        actRow.setOpaque(false);
        actRow.add(createIconPanel(AllIcons.Actions.Copy), BorderLayout.WEST);
        actRow.add(actualResultField, BorderLayout.CENTER);
        actRow.setBorder(JBUI.Borders.emptyTop(8));
        panel.add(actRow);

        return panel;
    }

    @Override
    public JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JBPanel<?> contentPanel) {
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

}
