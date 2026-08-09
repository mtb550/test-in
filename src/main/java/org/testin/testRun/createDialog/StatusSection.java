package org.testin.testRun.createDialog;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestStatus;
import org.testin.mappers.TestRunItems;
import org.testin.testRun.updateDialog.RunItemEditSection;

import javax.swing.*;
import java.awt.*;

public class StatusSection implements RunItemEditSection {

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    @Getter
    private final ComboBox<TestStatus> statusCombo;
    private final JBPanel<?> wrapper;

    public StatusSection() {
        this.statusCombo = new ComboBox<>(TestStatus.values());
        this.statusCombo.setFont(fieldFont);
        this.statusCombo.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(AllIcons.General.Filter), BorderLayout.WEST);
        this.wrapper.add(statusCombo, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JBPanel<?> contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        statusCombo.requestFocus();
    }

    @Override
    public void fillData(final @NotNull TestRunItems runItem) {
        statusCombo.setSelectedItem(runItem.getStatus());
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        if (wrapper.getParent() != null) {
            Object selected = statusCombo.getSelectedItem();
            if (selected instanceof TestStatus status) {
                runItem.setStatus(status);
            }
        }
    }

    @Override
    public JComponent getFocusComponent() {
        return statusCombo;
    }

}
