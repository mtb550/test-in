package org.testin.ui.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.TestStatus;
import org.testin.ui.testRun.update.RunItemEditSection;

import javax.swing.*;
import java.awt.*;

public class StatusSection implements RunItemEditSection {

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    @Getter
    private final JComboBox<TestStatus> statusCombo;
    private final JPanel wrapper;

    public StatusSection() {
        this.statusCombo = new JComboBox<>(TestStatus.values());
        this.statusCombo.setFont(fieldFont);
        this.statusCombo.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(AllIcons.General.Filter), BorderLayout.WEST);
        this.wrapper.add(statusCombo, BorderLayout.CENTER);
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
