package org.testin.testRun.createDialog;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.Severity;
import org.testin.mappers.TestRunItems;
import org.testin.testRun.updateDialog.RunItemEditSection;

import javax.swing.*;
import java.awt.*;

public class SeveritySection implements RunItemEditSection {

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    @Getter
    private final JComboBox<Severity> severityCombo;
    private final JPanel wrapper;

    public SeveritySection() {
        this.severityCombo = new JComboBox<>(Severity.values());
        this.severityCombo.setFont(fieldFont);
        this.severityCombo.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(AllIcons.General.Filter), BorderLayout.WEST);
        this.wrapper.add(severityCombo, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        severityCombo.requestFocus();
    }

    @Override
    public void fillData(final @NotNull TestRunItems runItem) {
        severityCombo.setSelectedItem(runItem.getSeverity());
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        if (wrapper.getParent() != null) {
            Object selected = severityCombo.getSelectedItem();
            if (selected instanceof Severity severity) {
                runItem.setSeverity(severity);
            }
        }
    }

    @Override
    public JComponent getFocusComponent() {
        return severityCombo;
    }

}
