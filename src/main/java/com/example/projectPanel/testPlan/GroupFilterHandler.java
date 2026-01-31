package com.example.projectPanel.testPlan;

import com.example.pojo.GroupType;
import com.example.pojo.TestCase;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupFilterHandler {
    private final Set<GroupType> selectedGroups = new HashSet<>();
    @Setter
    private CheckboxTree checkboxTree;
    @Setter
    private List<CheckedTreeNode> allTestCaseNodes;

    public JComponent createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton groupButton = new JButton("Groups ▼");
        groupButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showGroupPopup(groupButton);
            }
        });
        toolbar.add(new JLabel("Filter by Groups:"));
        toolbar.add(groupButton);
        return toolbar;
    }

    private void showGroupPopup(JComponent anchor) {
        JBList<GroupType> groupList = new JBList<>(GroupType.values());
        groupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        groupList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JCheckBox checkBox = new JCheckBox(value.name(), selectedGroups.contains(value));
            checkBox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            checkBox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            checkBox.setFont(list.getFont());
            return checkBox;
        });

        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = groupList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    GroupType group = groupList.getModel().getElementAt(index);
                    toggleGroupSelection(group);
                    groupList.repaint();
                    autoCheckGroups();
                }
            }
        });

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(groupList), null)
                .setTitle("Select Groups")
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .createPopup();

        popup.showUnderneathOf(anchor);
    }

    private void toggleGroupSelection(GroupType group) {
        if (selectedGroups.contains(group)) {
            selectedGroups.remove(group);
        } else {
            selectedGroups.add(group);
        }
    }

    private void autoCheckGroups() {
        if (allTestCaseNodes != null) {
            for (CheckedTreeNode node : allTestCaseNodes) {
                Object userObject = node.getUserObject();
                if (userObject instanceof TestCase testCase && testCase.getGroups() != null) {
                    boolean shouldCheck = testCase.getGroups().stream()
                            .anyMatch(selectedGroups::contains);
                    node.setChecked(shouldCheck);
                }
            }
            checkboxTree.repaint();
        }
    }
}