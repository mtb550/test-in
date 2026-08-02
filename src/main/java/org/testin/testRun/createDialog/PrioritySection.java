package org.testin.testRun.createDialog;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.Priority;
import org.testin.mappers.TestRunItems;
import org.testin.testRun.updateDialog.RunItemEditSection;

import javax.swing.*;
import java.awt.*;

public class PrioritySection implements RunItemEditSection {

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    @Getter
    private final JComboBox<Priority> priorityCombo;
    private final JPanel wrapper;

    public PrioritySection() {
        this.priorityCombo = new JComboBox<>(Priority.values());
        this.priorityCombo.setFont(fieldFont);
        this.priorityCombo.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(AllIcons.General.Filter), BorderLayout.WEST);
        this.wrapper.add(priorityCombo, BorderLayout.CENTER);
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
        priorityCombo.requestFocus();
    }

    @Override
    public void fillData(final @NotNull TestRunItems runItem) {
        priorityCombo.setSelectedItem(runItem.getPriority());
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        if (wrapper.getParent() != null) {
            Object selected = priorityCombo.getSelectedItem();
            if (selected instanceof Priority priority) {
                runItem.setPriority(priority);
            }
        }
    }

    @Override
    public JComponent getFocusComponent() {
        return priorityCombo;
    }

}
