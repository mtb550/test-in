package org.testin.testRun.createDialog;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.BugPriority;
import org.testin.mappers.TestRunItems;
import org.testin.testRun.updateDialog.RunItemEditSection;

import javax.swing.*;
import java.awt.*;

public class BugPrioritySection implements RunItemEditSection {

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    @Getter
    private final JComboBox<BugPriority> bugPriorityCombo;
    private final JPanel wrapper;

    public BugPrioritySection() {
        this.bugPriorityCombo = new JComboBox<>(BugPriority.values());
        this.bugPriorityCombo.setFont(fieldFont);
        this.bugPriorityCombo.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(AllIcons.General.Filter), BorderLayout.WEST);
        this.wrapper.add(bugPriorityCombo, BorderLayout.CENTER);
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
        bugPriorityCombo.requestFocus();
    }

    @Override
    public void fillData(final @NotNull TestRunItems runItem) {
        bugPriorityCombo.setSelectedItem(runItem.getBugPriority());
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        if (wrapper.getParent() != null) {
            Object selected = bugPriorityCombo.getSelectedItem();
            if (selected instanceof BugPriority bugPriority) {
                runItem.setBugPriority(bugPriority);
            }
        }
    }

    @Override
    public JComponent getFocusComponent() {
        return bugPriorityCombo;
    }

}
