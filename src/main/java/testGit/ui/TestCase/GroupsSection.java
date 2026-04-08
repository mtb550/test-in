package testGit.ui.TestCase;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupsSection implements CreateTestCaseSection {
    private final JPanel groups;
    private final JPanel wrapper;
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

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateField.GROUPS.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.groups, BorderLayout.CENTER);
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
        for (Component c : groups.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                SwingUtilities.invokeLater(cb::requestFocusInWindow);
                return;
            }
        }
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            ArrayList<Groups> selectedGroups = new ArrayList<>();
            for (Component c : groups.getComponents()) {
                if (c instanceof JBCheckBox cb && cb.isSelected()) {
                    selectedGroups.add(Groups.valueOf(cb.getText()));
                }
            }
            dto.setGroups(selectedGroups.isEmpty() ? null : selectedGroups);
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.UIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseGroups.getShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        for (Component c : groups.getComponents()) {
            if (c instanceof JBCheckBox) {
                return (JComponent) c;
            }
        }
        return groups;
    }

    @Override
    public void setEditable(final boolean editable) {
        for (Component c : groups.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                cb.setEnabled(editable);
            }
        }
    }

    public void setSelectedGroups(final List<Groups> selectedList) {
        if (selectedList == null) return;
        for (Component c : groups.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                try {
                    Groups group = Groups.valueOf(cb.getText());
                    cb.setSelected(selectedList.contains(group));
                } catch (Exception ignored) {
                    System.out.println("setSelectedGroups ignored");
                }
            }
        }
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.UIAction repackAction) {
        setSelectedGroups(dto.getGroups());
    }
}