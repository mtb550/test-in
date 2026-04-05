package testGit.ui.single.nnew;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Groups;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class GroupsSection {
    private final JPanel groups;
    private final JPanel wrapperPanel;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 1f);


    public GroupsSection() {
        this.groups = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        this.groups.setOpaque(false);

        Arrays.stream(Groups.values())
                .filter(Groups::isActive)
                .map(group -> {
                    JBCheckBox checkBox = new JBCheckBox(group.name());
                    checkBox.setFont(fieldFont);
                    return checkBox;
                })
                .forEach(this.groups::add);

        this.wrapperPanel = new JPanel(new BorderLayout());
        this.wrapperPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(CreateField.GROUPS.getIcon());
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        this.wrapperPanel.add(iconLabel, BorderLayout.WEST);
        this.wrapperPanel.add(this.groups, BorderLayout.CENTER);
        this.wrapperPanel.setBorder(JBUI.Borders.emptyTop(8));
    }

    public void showSection(JPanel contentPanel) {
        if (wrapperPanel.getParent() == null)
            contentPanel.add(wrapperPanel);
        focusFirstCheckbox();
    }

    private void focusFirstCheckbox() {
        for (Component c : groups.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                SwingUtilities.invokeLater(cb::requestFocusInWindow);
                return;
            }
        }
    }

    public JPanel getInnerPanel() {
        return groups;
    }

    public JPanel getWrapper() {
        return wrapperPanel;
    }
}