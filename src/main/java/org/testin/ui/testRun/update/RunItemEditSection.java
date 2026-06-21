package org.testin.ui.testRun.update;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestRunItems;

import javax.swing.*;
import java.awt.*;

public interface RunItemEditSection {

    JPanel getWrapper();

    void showSection(final JPanel contentPanel);

    void fillData(final @NotNull TestRunItems runItem);

    void applyTo(final @NotNull TestRunItems runItem);

    JComponent getFocusComponent();

    default JPanel createIconPanel(final Icon icon) {
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}
