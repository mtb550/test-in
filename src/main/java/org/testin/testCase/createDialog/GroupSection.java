package org.testin.testCase.createDialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.testin.enums.CreateTestCaseFields;
import org.testin.enums.Group;
import org.testin.enums.IUIAction;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupSection implements ICreateTestCaseSection {
    private final JBPanel<?> group;
    private final JBPanel<?> wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 1f);

    public GroupSection() {
        this.group = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        this.group.setOpaque(false);

        Arrays.stream(Group.values())
                .filter(Group::isActive)
                .map(group -> {
                    JBCheckBox checkBox = new JBCheckBox(group.name());
                    checkBox.setFont(fieldFont);
                    return checkBox;
                })
                .forEach(this.group::add);

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.GROUP.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.group, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JBPanel<?> contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        focusFirstCheckbox();
    }

    private void focusFirstCheckbox() {
        for (Component c : group.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                ApplicationManager.getApplication().invokeLater(cb::requestFocusInWindow);
                return;
            }
        }
    }

    @Override
    public void applyTo(final TestCaseDto dto) {
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
    public void setupShortcut(final JComponent mainPanel, final JBPanel<?> slot, final TestCaseBaseDialog base, final IUIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseGroup.getCustomShortcut(), () -> {
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

                } catch (final Exception ex) {
                    Logger.error("setSelectedGroups ignored");
                }
            }
        }
    }

    @Override
    public void fillData(final TestCaseDto dto, final IUIAction repackAction) {
        setSelectedGroup(dto.getGroup());
    }
}