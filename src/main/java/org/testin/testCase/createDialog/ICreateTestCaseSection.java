package org.testin.testCase.createDialog;

import com.intellij.ui.components.JBLabel;


import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.testin.enums.IUIAction;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public interface ICreateTestCaseSection {
    JBPanel<?> getWrapper();

    void showSection(final JBPanel<?> contentPanel);

    void applyTo(final TestCaseDto dto);

    void setupShortcut(final JComponent mainPanel, final JBPanel<?> slot, final TestCaseBaseDialog base, final IUIAction repackAction);

    JComponent getFocusComponent();

    void setEditable(final boolean editable);

    void fillData(final TestCaseDto dto, final IUIAction repackAction);

    default JBPanel<?> createIconPanel(final Icon icon) {
        JBPanel<?> iconPanel = new JBPanel<>(new GridBagLayout());
        iconPanel.setOpaque(false);
        JBLabel iconLabel = new JBLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}