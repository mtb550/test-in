package com.example.explorer;

import com.example.pojo.GroupType;
import com.example.pojo.TestCase;
import com.example.pojo.TestPlan;
import com.example.pojo.Tree;
import com.example.util.NodeType;
import com.example.util.sql;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class TestPlanPopup {

    public static void showFolderInfo(TestPlan plan, JComponent parent) {
        DialogWrapper dialog = new DialogWrapper(true) {
            private CheckboxTree checkboxTree;
            private CheckedTreeNode rootNode;
            private JTextField buildField;
            private List<CheckedTreeNode> allTestCaseNodes = new ArrayList<>();
            private Set<GroupType> selectedGroups = new HashSet<>();

            {
                init();
                setTitle("Add Test Cases to Plan");
            }

            @Override
            protected JComponent createCenterPanel() {
                JBPanel<?> panel = new JBPanel<>(new BorderLayout(10, 10));
                panel.setPreferredSize(new Dimension(550, 600));

                // Build number
                JBPanel<?> buildPanel = new JBPanel<>(new BorderLayout(5, 5));
                buildPanel.add(new JBLabel("🔢 Build Number:"), BorderLayout.NORTH);
                buildField = new JTextField();
                buildPanel.add(buildField, BorderLayout.CENTER);
                panel.add(buildPanel, BorderLayout.NORTH);

                // Group selector toolbar
                JBPanel<?> toolbar = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
                JButton groupButton = new JButton("Groups ▼");
                groupButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        showGroupPopup(groupButton);
                    }
                });
                toolbar.add(new JBLabel("Filter by Groups:"));
                toolbar.add(groupButton);
                panel.add(toolbar, BorderLayout.BEFORE_FIRST_LINE);

                // Load test case tree
                rootNode = new CheckedTreeNode("Test Cases");
                Tree root = new sql().get("SELECT * FROM tree WHERE id = ?", plan.getProject_id()).as(Tree.class);
                if (root != null) {
                    buildTreeRecursive(root, rootNode);
                }

                checkboxTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
                    @Override
                    public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
                        if (value instanceof CheckedTreeNode ctNode) {
                            Object userObject = ctNode.getUserObject();
                            if (userObject instanceof Tree treeNode) {
                                getTextRenderer().append(treeNode.getName());
                            } else if (userObject instanceof TestCase testCase) {
                                getTextRenderer().append("🧪 " + testCase.getTitle());
                                if (testCase.getGroups() != null && !testCase.getGroups().isEmpty()) {
                                    getTextRenderer().append("  [");
                                    for (int i = 0; i < testCase.getGroups().size(); i++) {
                                        getTextRenderer().append(testCase.getGroups().get(i).name());
                                        if (i < testCase.getGroups().size() - 1)
                                            getTextRenderer().append(", ");
                                    }
                                    getTextRenderer().append("]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                                }
                            } else {
                                getTextRenderer().append(value.toString());
                            }
                        }
                    }
                }, rootNode);

                // Expand all
                for (int i = 0; i < checkboxTree.getRowCount(); i++) {
                    checkboxTree.expandRow(i);
                }

                JBScrollPane scrollPane = new JBScrollPane(checkboxTree);
                scrollPane.setPreferredSize(new Dimension(500, 380));
                panel.add(scrollPane, BorderLayout.CENTER);

                // configuration
                JBPanel<?> configPanel = new JBPanel<>(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = JBUI.insets(4, 8);
                //gbc.anchor = GridBagConstraints.WEST;           // ⬅️ Align left
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 1;

                // Platform
                JBLabel platformLabel = new JBLabel("🧱 Platform:");
                ComboBox<String> platformCombo = new ComboBox<>(new String[]{"", "api", "web", "mobile"});
                configPanel.add(platformLabel, gbc);
                gbc.gridx++;
                configPanel.add(platformCombo, gbc);

                // Language
                gbc.gridx = 0;
                gbc.gridy++;
                JBLabel languageLabel = new JBLabel("🌍 Language:");
                ComboBox<String> languageCombo = new ComboBox<>(new String[]{"", "en", "ar"});
                configPanel.add(languageLabel, gbc);
                gbc.gridx++;
                configPanel.add(languageCombo, gbc);

                // Browser
                gbc.gridx = 0;
                gbc.gridy++;
                JBLabel browserLabel = new JBLabel("🌐 Browser:");
                browserLabel.setToolTipText("Hidden when platform is API or Mobile");
                ComboBox<String> browserCombo = new ComboBox<>(new String[]{"", "chrome", "safari", "edge", "firefox"});
                configPanel.add(browserLabel, gbc);
                gbc.gridx++;
                configPanel.add(browserCombo, gbc);

                // Device Type
                gbc.gridx = 0;
                gbc.gridy++;
                JBLabel deviceLabel = new JBLabel("📱 Device Type:");
                deviceLabel.setToolTipText("Hidden when platform is API or Web");
                ComboBox<String> deviceCombo = new ComboBox<>(new String[]{"", "iPhone", "Android", "Huawei"});
                configPanel.add(deviceLabel, gbc);
                gbc.gridx++;
                configPanel.add(deviceCombo, gbc);


                panel.add(configPanel, BorderLayout.AFTER_LAST_LINE);

                // Behavior: Disable browser/device if platform = api
                platformCombo.addActionListener(e -> {
                    String selected = (String) platformCombo.getSelectedItem();
                    boolean isApi = "api".equalsIgnoreCase(selected);
                    boolean isWeb = "web".equalsIgnoreCase(selected);
                    boolean isMobile = "mobile".equalsIgnoreCase(selected);

                    browserCombo.setVisible(!isApi && !isMobile);
                    browserLabel.setVisible(!isApi && !isMobile);
                    deviceCombo.setVisible(!isApi && !isWeb);
                    deviceLabel.setVisible(!isApi && !isWeb);
                });


                return panel;
            }

            private void showGroupPopup(JComponent anchor) {
                // Create JBList with all groups
                JBList<GroupType> groupList = new JBList<>(GroupType.values());
                groupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

                // Custom cell renderer with checkbox
                groupList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
                    JCheckBox checkBox = new JCheckBox(value.name(), selectedGroups.contains(value));
                    checkBox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                    checkBox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
                    checkBox.setFont(list.getFont());
                    return checkBox;
                });

                // Toggle selectedGroups when clicked
                groupList.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int index = groupList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            GroupType group = groupList.getModel().getElementAt(index);
                            if (selectedGroups.contains(group)) {
                                selectedGroups.remove(group);
                            } else {
                                selectedGroups.add(group);
                            }
                            groupList.repaint();
                            autoCheckGroups(); // update the tree view
                        }
                    }
                });

                // Create popup from JBList
                JBPopup popup = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(new JBScrollPane(groupList), null)
                        .setTitle("Select Groups")
                        .setResizable(true)
                        .setMovable(true)
                        .setRequestFocus(true)
                        .createPopup();

                popup.showUnderneathOf(anchor);
            }

            private void autoCheckGroups() {
                for (CheckedTreeNode node : allTestCaseNodes) {
                    Object userObject = node.getUserObject();
                    if (userObject instanceof TestCase testCase && testCase.getGroups() != null) {
                        boolean shouldCheck = testCase.getGroups().stream().anyMatch(selectedGroups::contains);
                        node.setChecked(shouldCheck);
                    }
                }
                checkboxTree.repaint();
            }

            private void onAdd(TestPlan plan) {
                String buildNumber = buildField.getText().trim();
                if (buildNumber.isBlank()) {
                    Messages.showWarningDialog("Please enter a build number.", "Validation");
                    return;
                }

                List<String> selectedCaseIds = new ArrayList<>();
                collectSelectedTestCases(rootNode, selectedCaseIds);

                if (selectedCaseIds.isEmpty()) {
                    Messages.showWarningDialog("Please select at least one test case.", "Validation");
                    return;
                }

                sql db = new sql();
                db.execute("UPDATE nafath_tp_tree SET build_number = ? WHERE id = ?", buildNumber, plan.getId());

                for (int i = 0; i < selectedCaseIds.size(); i++) {
                    db.execute("INSERT INTO nafath_tp_testcases (plan_id, test_case_id, run_order) VALUES (?, ?, ?)",
                            plan.getId(), selectedCaseIds.get(i), i + 1);
                }

                Messages.showInfoMessage("Test cases added to plan successfully.", "Success");
                close(OK_EXIT_CODE);
            }

            private void buildTreeRecursive(Tree treeItem, CheckedTreeNode parentNode) {
                CheckedTreeNode currentNode = new CheckedTreeNode(treeItem);
                currentNode.setChecked(false);
                parentNode.add(currentNode);

                if (treeItem.getType() == NodeType.FEATURE.getCode()) {
                    TestCase[] testCases = new sql().get(
                            "SELECT * FROM nafath_tc WHERE module = ? ORDER BY sort", treeItem.getId()
                    ).as(TestCase[].class);

                    for (TestCase tc : testCases) {
                        CheckedTreeNode testCaseNode = new CheckedTreeNode(tc);
                        testCaseNode.setChecked(false);
                        allTestCaseNodes.add(testCaseNode);
                        currentNode.add(testCaseNode);
                    }

                    return;
                }

                Tree[] children = new sql().get("SELECT * FROM tree WHERE link = ?", treeItem.getId()).as(Tree[].class);
                for (Tree child : children) {
                    buildTreeRecursive(child, currentNode);
                }
            }

            private void collectSelectedTestCases(CheckedTreeNode node, List<String> output) {
                Enumeration<?> enumeration = node.children();
                while (enumeration.hasMoreElements()) {
                    Object child = enumeration.nextElement();
                    if (child instanceof CheckedTreeNode ctNode) {
                        Object userObject = ctNode.getUserObject();
                        if (ctNode.isChecked() && userObject instanceof TestCase testCase) {
                            output.add(testCase.getId());
                        }
                        collectSelectedTestCases(ctNode, output);
                    }
                }
            }

            private class GroupTypeRenderer implements ListCellRenderer<GroupType> {
                @Override
                public Component getListCellRendererComponent(JList<? extends GroupType> list, GroupType value,
                                                              int index, boolean isSelected, boolean cellHasFocus) {
                    JCheckBox checkBox = new JCheckBox(value.name(), selectedGroups.contains(value));
                    checkBox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                    checkBox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
                    checkBox.setFont(list.getFont());
                    return checkBox;
                }
            }
        };

        dialog.show();
    }
}