package org.testin.testRun.createDialog;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.BugSeverity;
import org.testin.mappers.TestRunItems;
import org.testin.testRun.updateDialog.RunItemEditSection;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;

public class BugSeveritySection implements RunItemEditSection {

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    @Getter
    private final ButtonGroup buttonGroup;
    private final JPanel wrapper;
    private BugSeverity selected;

    public BugSeveritySection() {
        this.buttonGroup = new ButtonGroup();

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radioPanel.setOpaque(false);

        for (BugSeverity bs : BugSeverity.values()) {
            JRadioButton rb = new JRadioButton(bs.getName());
            rb.setFont(fieldFont);
            rb.setOpaque(false);
            rb.setActionCommand(bs.name());
            rb.addActionListener(e -> selected = BugSeverity.valueOf(e.getActionCommand()));
            buttonGroup.add(rb);
            radioPanel.add(rb);
        }

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(AllIcons.General.Filter), BorderLayout.WEST);
        this.wrapper.add(radioPanel, BorderLayout.CENTER);
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
        BugSeverity bs = runItem.getBugSeverity();
        selected = bs;
        Enumeration<AbstractButton> e = buttonGroup.getElements();
        while (e.hasMoreElements()) {
            AbstractButton b = e.nextElement();
            if (b.getActionCommand().equals(bs.name())) {
                b.setSelected(true);
                break;
            }
        }
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        if (wrapper.getParent() != null && selected != null) {
            runItem.setBugSeverity(selected);
        }
    }

    @Override
    public JComponent getFocusComponent() {
        return wrapper;
    }

}
