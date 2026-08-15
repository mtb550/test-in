package org.testin.testcase.createDialog;

import com.intellij.ui.components.JBLabel;


import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.UIAction;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public interface CreateTestCaseSection {
    @NotNull JBPanel<?> getWrapper();

    void showSection(final @NotNull JBPanel<?> contentPanel);

    void applyTo(final @NotNull TestCaseDto dto);

    void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction);

    @NotNull JComponent getFocusComponent();

    void setEditable(final boolean editable);

    void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction);

    default @NotNull JBPanel<?> createIconPanel(final @NotNull Icon icon) {
        final JBPanel<?> iconPanel = new JBPanel<>(new GridBagLayout());
        iconPanel.setOpaque(false);
        final JBLabel iconLabel = new JBLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}