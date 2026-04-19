package testGit.ui.TestCase;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Group;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupSection implements ICreateTestCaseSection {
    private final JPanel group;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 1f);

    public GroupSection() {
        this.group = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        this.group.setOpaque(false);

        Arrays.stream(Group.values())
                .filter(Group::isActive)
                .map(group -> {
                    JBCheckBox checkBox = new JBCheckBox(group.name());
                    checkBox.setFont(fieldFont);
                    return checkBox;
                })
                .forEach(this.group::add);

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.GROUP.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.group, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        focusFirstCheckbox();
    }

    private void focusFirstCheckbox() {
        for (Component c : group.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                SwingUtilities.invokeLater(cb::requestFocusInWindow);
                return;
            }
        }
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            ArrayList<Group> selectedGroups = new ArrayList<>();
            for (Component c : group.getComponents()) {
                if (c instanceof JBCheckBox cb && cb.isSelected()) {
                    selectedGroups.add(Group.valueOf(cb.getText()));
                }
            }
            dto.setGroup(selectedGroups);
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.IUIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseGroup.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        for (Component c : group.getComponents()) {
            if (c instanceof JBCheckBox) {
                return (JComponent) c;
            }
        }
        return group;
    }

    @Override
    public void setEditable(final boolean editable) {
        for (Component c : group.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                cb.setEnabled(editable);
            }
        }
    }

    public void setSelectedGroup(final List<Group> selectedList) {
        if (selectedList == null) return;
        for (Component c : group.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                try {
                    Group group = Group.valueOf(cb.getText());
                    cb.setSelected(selectedList.contains(group));
                } catch (Exception ignored) {
                    System.out.println("setSelectedGroups ignored");
                }
            }
        }
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.IUIAction repackAction) {
        setSelectedGroup(dto.getGroup());
    }
}