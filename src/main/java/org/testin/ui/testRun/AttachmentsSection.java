package org.testin.ui.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestRunItems;
import org.testin.ui.testRun.update.RunItemEditSection;

import javax.swing.*;
import java.awt.*;

public class AttachmentsSection implements RunItemEditSection {

    final Font labelFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    private final JPanel wrapper;
    private final JBLabel placeholderLabel;

    public AttachmentsSection() {
        this.placeholderLabel = new JBLabel("Attachments support coming soon");
        this.placeholderLabel.setFont(labelFont);

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(AllIcons.FileTypes.Text), BorderLayout.WEST);
        this.wrapper.add(placeholderLabel, BorderLayout.CENTER);
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
    }

    @Override
    public void fillData(final @NotNull TestRunItems runItem) {
        // placeholder - no data to fill
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        // placeholder - no data to apply
    }

    @Override
    public JComponent getFocusComponent() {
        return placeholderLabel;
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
