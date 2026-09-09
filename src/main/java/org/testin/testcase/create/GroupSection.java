package org.testin.testcase.create;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GroupSection implements CreateTestCaseSection {
    /**
     * The tick box for each group, and the group it stands for.
     * <p>
     * A map rather than the box's own text. The text used to be the group's
     * identity - {@code Group.valueOf(cb.getText())} - so it had to be the enum
     * constant, and the tester read REGRESSION where every other surface says
     * Regression (#199). A label is what a tester reads; it is not a key.
     */
    private final @NotNull Map<Group, JBCheckBox> boxes = new LinkedHashMap<>();
    private final @NotNull JBPanel<?> group;
    private final @NotNull JBPanel<?> wrapper;

    public GroupSection() {
        this.group = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        this.group.setOpaque(false);

        // Every group, in the enum's own order - No Group first, then the
        // groups themselves. It was left out entirely once, so a tester who
        // ticked a group by mistake had no way back and an import carrying no
        // group could not round-trip (#265); and four more were left out after
        // that, so a case could arrive carrying a group nobody could type
        // (#200).
        Arrays.stream(Group.values())
                .forEach(g -> {
                    final @NotNull JBCheckBox checkBox = new JBCheckBox(g.getName());
                    checkBox.setFont(fieldFont());
                    checkBox.addActionListener(e -> keepExclusive(g));

                    boxes.put(g, checkBox);
                    this.group.add(checkBox);
                });

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
        // No Group is the empty list, which is what every reader already treats
        // as unassigned - so it is a way of saying nothing rather than a group
        // to store.
        if (boxes.get(Group.UNASSIGNED).isSelected()) {
            dto.setGroup(new ArrayList<>());
            return;
        }

        final @NotNull ArrayList<Group> selectedGroups = new ArrayList<>();
        boxes.forEach((g, cb) -> {
            if (cb.isSelected()) selectedGroups.add(g);
        });

        dto.setGroup(selectedGroups);
    }

    /**
     * No Group is not a group, so it cannot be ticked beside one.
     */
    private void keepExclusive(final @NotNull Group justToggled) {
        if (!boxes.get(justToggled).isSelected()) return;

        if (justToggled == Group.UNASSIGNED) {
            boxes.forEach((g, cb) -> cb.setSelected(g == Group.UNASSIGNED));
            return;
        }

        boxes.get(Group.UNASSIGNED).setSelected(false);
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
        boxes.forEach((g, cb) -> cb.setSelected(g == Group.UNASSIGNED
                ? selectedList.isEmpty()
                : selectedList.contains(g)));
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        setSelectedGroup(dto.getGroup());
    }
}