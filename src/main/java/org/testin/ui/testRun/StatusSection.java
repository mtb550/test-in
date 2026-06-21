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

    private static final float FONT_SIZE_OFFSET = 4f;

    @Getter
    private final JComboBox<TestStatus> statusCombo;

    private final JPanel wrapper;

    public StatusSection() {
        this.statusCombo = new JComboBox<>(TestStatus.values());
        this.statusCombo.setFont(JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + FONT_SIZE_OFFSET));
        this.statusCombo.setBorder(JBUI.Borders.empty(10));

        this.wrapper = buildPanel();
    }

    private JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(createIconPanel(AllIcons.General.Filter), BorderLayout.WEST);
        panel.add(statusCombo, BorderLayout.CENTER);
        panel.setBorder(JBUI.Borders.emptyTop(8));
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
