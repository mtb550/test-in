package org.testin.testcase.create;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupSection implements CreateTestCaseSection {
    private final @NotNull JBPanel<?> group;
    private final @NotNull JBPanel<?> wrapper;

    public GroupSection() {
        this.group = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        this.group.setOpaque(false);

        Arrays.stream(Group.values())
                .filter(Group::isActive)
                .map(group -> {
                    final @NotNull JBCheckBox checkBox = new JBCheckBox(group.name());
                    checkBox.setFont(fieldFont());
                    return checkBox;
                })
                .forEach(this.group::add);

        this.wrapper = createWrapper(CreateTestCaseFields.GROUP.getIcon(), this.group);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void focusOnShow() {
        focusFirstCheckbox();
    }

    private void focusFirstCheckbox() {
        for (final Component c : group.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                ApplicationManager.getApplication().invokeLater(cb::requestFocusInWindow);
                return;
            }
        }
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        final @NotNull ArrayList<Group> selectedGroups = new ArrayList<>();
        for (final Component c : group.getComponents()) {
            if (c instanceof JBCheckBox cb && cb.isSelected()) {
                selectedGroups.add(Group.valueOf(cb.getText()));
            }
        }
        dto.setGroup(selectedGroups);
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseGroup.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        for (final Component c : group.getComponents()) {
            if (c instanceof JBCheckBox) {
                return (JComponent) c;
            }
        }
        return group;
    }

    @Override
    public void setEditable(final boolean editable) {
        for (final Component c : group.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                cb.setEnabled(editable);
            }
        }
    }

    public void setSelectedGroup(final @NotNull List<Group> selectedList) {
        for (final Component c : group.getComponents()) {
            if (c instanceof JBCheckBox cb) {
                try {
                    final @NotNull Group group = Group.valueOf(cb.getText());
                    cb.setSelected(selectedList.contains(group));

                } catch (final Exception ex) {
                    Logger.error("setSelectedGroups ignored");
                }
            }
        }
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        setSelectedGroup(dto.getGroup());
    }
}