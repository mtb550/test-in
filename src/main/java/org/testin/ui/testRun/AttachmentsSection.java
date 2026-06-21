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

    private final JPanel wrapper;
    private final JBLabel placeholderLabel;

    public AttachmentsSection() {
        this.placeholderLabel = new JBLabel("Attachments support coming soon");
        this.placeholderLabel.setFont(JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f));
        this.placeholderLabel.setBorder(JBUI.Borders.empty(10));

        this.wrapper = buildPanel();
    }

    private JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(createIconPanel(AllIcons.FileTypes.Text), BorderLayout.WEST);
        panel.add(placeholderLabel, BorderLayout.CENTER);
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
}
