package org.testin.testRun.updateDialog;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.TestRunItems;

import javax.swing.*;
import java.awt.*;

public interface RunItemEditSection {

    JBPanel<?> getWrapper();

    void showSection(final JBPanel<?> contentPanel);

    void fillData(final @NotNull TestRunItems runItem);

    void applyTo(final @NotNull TestRunItems runItem);

    JComponent getFocusComponent();

    default JBPanel<?> createIconPanel(final Icon icon) {
        JBPanel<?> iconPanel = new JBPanel<>(new GridBagLayout());
        iconPanel.setOpaque(false);
        JBLabel iconLabel = new JBLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}
