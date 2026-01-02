package com.example.viewer;

import com.example.pojo.GroupType;
import com.example.pojo.TestCase;
import com.example.util.ActionHistory;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AddTestCasePanel {
    @Getter
    private final JBPanel<?> mainPanel;
    private final JBPanel<?> formPanel;

    private JBTextField titleField;
    private JBTextArea expectedArea;
    private JBTextArea stepsArea;
    private JBTextField priorityField;
    private JBTextField autoRefField;
    private JBTextField busiRefField;
    private JBTextField groupsField;
    private JBTextField moduleField;

    private JButton saveButton;
    private JButton cancelButton;
    @Setter
    private Consumer<TestCase> onSaveCallback;

    public AddTestCasePanel() {
        mainPanel = new JBPanel<>(new BorderLayout());
        formPanel = new JBPanel<>(new GridBagLayout());

        initializeForm();

        JBScrollPane scrollPane = new JBScrollPane(formPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // ESC to cancel/clear
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancelAdd");
        mainPanel.getActionMap().put("cancelAdd", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
    }

    private void initializeForm() {
        formPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(8, 16);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Initialize fields with empty values
        titleField = new JBTextField();
        titleField.setToolTipText("Enter test case title");

        expectedArea = new JBTextArea(3, 40);
        expectedArea.setLineWrap(true);
        expectedArea.setWrapStyleWord(true);
        expectedArea.setToolTipText("Enter expected result");

        stepsArea = new JBTextArea(5, 40);
        stepsArea.setLineWrap(true);
        stepsArea.setWrapStyleWord(true);
        stepsArea.setToolTipText("Enter test steps");

        priorityField = new JBTextField();
        priorityField.setToolTipText("e.g., High, Medium, Low");

        autoRefField = new JBTextField();
        autoRefField.setToolTipText("Enter automation reference");

        busiRefField = new JBTextField();
        busiRefField.setToolTipText("Enter business reference");

        groupsField = new JBTextField();
        groupsField.setToolTipText("Enter groups (comma-separated): Regression, Smoke, Sanity, Security, UI, Functional, Validation");

        moduleField = new JBTextField();
        moduleField.setToolTipText("Enter module name");

        // Add form rows
        addRow("📝 Title: *", titleField, formPanel, gbc, row++);
        addRow("🎯 Expected Result: *", new JBScrollPane(expectedArea), formPanel, gbc, row++);
        addRow("🪜 Steps: *", new JBScrollPane(stepsArea), formPanel, gbc, row++);
        addRow("🏷 Priority:", priorityField, formPanel, gbc, row++);
        addRow("📁 Module:", moduleField, formPanel, gbc, row++);
        addRow("🤖 Automation Ref:", autoRefField, formPanel, gbc, row++);
        addRow("📊 Business Ref:", busiRefField, formPanel, gbc, row++);
        addRow("🧪 Groups:", groupsField, formPanel, gbc, row++);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        saveButton = new JButton("Create Test Case");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.addActionListener(e -> onSave());
        saveButton.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "save");
        saveButton.getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSave();
            }
        });

        cancelButton = new JButton("Clear");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelButton.addActionListener(e -> onCancel());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(buttonPanel, gbc);

        // Focus on title field
        SwingUtilities.invokeLater(() -> titleField.requestFocusInWindow());

        formPanel.revalidate();
        formPanel.repaint();
    }

    private void onSave() {
        // Validate required fields
        String title = titleField.getText().trim();
        String expected = expectedArea.getText().trim();
        String steps = stepsArea.getText().trim();

        if (title.isEmpty() || expected.isEmpty() || steps.isEmpty()) {
            JOptionPane.showMessageDialog(
                    mainPanel,
                    "⚠️ Please fill in all required fields (Title, Expected Result, Steps).",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Create new test case
        TestCase newTestCase = new TestCase();
        newTestCase.setTitle(title);
        newTestCase.setExpectedResult(expected);
        newTestCase.setSteps(steps);
        newTestCase.setPriority(priorityField.getText().trim());
        newTestCase.setAutomationRef(autoRefField.getText().trim());
        newTestCase.setBusinessRef(busiRefField.getText().trim());
        newTestCase.setModule(moduleField.getText().trim());

        // Parse groups - convert strings to GroupType enum values
        String groupsText = groupsField.getText().trim();
        if (!groupsText.isEmpty()) {
            List<GroupType> groupTypes = Arrays.stream(groupsText.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(groupName -> {
                        try {
                            return GroupType.valueOf(groupName);
                        } catch (IllegalArgumentException e) {
                            System.err.println("Invalid group type: " + groupName);
                            return null;
                        }
                    })
                    .filter(gt -> gt != null)
                    .collect(Collectors.toList());

            if (!groupTypes.isEmpty()) {
                newTestCase.setGroups(groupTypes);
            }
        }

        // Set metadata
        newTestCase.setCreateBy("current_user"); // Replace with actual user
        newTestCase.setUpdateBy("current_user");
        newTestCase.setCreateAt(LocalDateTime.now());
        newTestCase.setUpdateAt(LocalDateTime.now());
        newTestCase.setValidFrom(LocalDateTime.now());

        // Register undo/redo action
        ActionHistory.register(
                () -> {
                    System.out.println("[UNDO] Test case creation reverted");
                },
                () -> {
                    System.out.println("[REDO] Test case created again");
                }
        );

        // Call the callback if provided
        if (onSaveCallback != null) {
            onSaveCallback.accept(newTestCase);
            // Clear form after successful save when using callback
            clearForm();
        } else {
            JOptionPane.showMessageDialog(
                    mainPanel,
                    "✅ Test case created successfully!\n\nTitle: " + title,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
            // Clear form for next entry
            clearForm();
        }
    }

    private void onCancel() {
        if (onSaveCallback != null) {
            // If using callback mode, just clear the form
            clearForm();
        } else {
            int result = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "Are you sure you want to clear all fields?",
                    "Confirm Clear",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                clearForm();
            }
        }
    }

    private void clearForm() {
        titleField.setText("");
        expectedArea.setText("");
        stepsArea.setText("");
        priorityField.setText("");
        autoRefField.setText("");
        busiRefField.setText("");
        groupsField.setText("");
        moduleField.setText("");
        SwingUtilities.invokeLater(() -> titleField.requestFocusInWindow());
    }

    private void addRow(String label, JComponent input, JPanel panel, GridBagConstraints gbc, int row) {
        JBLabel keyLabel = new JBLabel(label);
        keyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        keyLabel.setHorizontalAlignment(SwingConstants.LEFT);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(keyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(input, gbc);
    }

}