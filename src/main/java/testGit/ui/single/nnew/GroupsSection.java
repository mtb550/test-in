package testGit.ui.single.nnew;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Groups;
import testGit.ui.bulk.UpdateField;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class GroupsSection {
    private final JPanel innerGroupsPanel;
    private final JPanel wrapperPanel;
    private final float fontSize = JBUI.Fonts.label().getSize2D() + 1f; // أصغر قليلاً

    public GroupsSection() {
        // 1. بناء لوحة المجموعات الداخلية (Checkboxes)
        this.innerGroupsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        this.innerGroupsPanel.setOpaque(false);

        Arrays.stream(Groups.values())
                .filter(Groups::isActive)
                .map(group -> {
                    JBCheckBox checkBox = new JBCheckBox(group.name());
                    checkBox.setFont(JBFont.regular().deriveFont(fontSize));
                    return checkBox;
                })
                .forEach(this.innerGroupsPanel::add);

        // 2. تغليف اللوحة في JPanel مع إضافة الأيقونة الجانبية
        this.wrapperPanel = new JPanel(new BorderLayout());
        this.wrapperPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UpdateField.GROUPS.getIcon());
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));

        this.wrapperPanel.add(iconLabel, BorderLayout.WEST);
        this.wrapperPanel.add(this.innerGroupsPanel, BorderLayout.CENTER);
        this.wrapperPanel.setBorder(JBUI.Borders.emptyTop(8));
    }

    public Runnable getShowAction(JPanel contentPanel, Runnable repackPopup) {
        return () -> {
            if (wrapperPanel.getParent() == null) {
                contentPanel.add(wrapperPanel);
            }
            repackPopup.run();
            focusFirstCheckbox();
        };
    }

    // تم نقل هذه الدالة إلى هنا لتكون خاصة بهذه الفئة!
    private void focusFirstCheckbox() {
        for (Component c : innerGroupsPanel.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                SwingUtilities.invokeLater(cb::requestFocusInWindow);
                return;
            }
        }
    }

    public JPanel getInnerPanel() {
        return innerGroupsPanel;
    }

    public JPanel getWrapper() {
        return wrapperPanel;
    }
}