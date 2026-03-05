package testGit.viewPanel;

import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import testGit.pojo.DB;
import testGit.pojo.Priority;
import testGit.pojo.TestCase;
import testGit.pojo.TestCaseHistory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TestCaseDetailsPanel {
    private final JBPanel<?> mainPanel;
    private final JBTabbedPane tabbedPane;
    private final JBPanel<?> detailTab;
    private final JBPanel<?> historyTab;
    private final JBPanel<?> bugTab;
    private JBTextField titleField;
    private JBTextField expectedArea;
    private JBTextField stepsArea;
    private JBTextField priorityField;
    private JBTextField autoRefField;
    private JBTextField busiRefField;
    private JBTextField groupsField;

    private JBLabel idLabel;
    private JBLabel titleLabel;
    private JBLabel expectedLabel;
    private JBLabel stepsLabel;
    private JBLabel priorityLabel;
    private JBLabel autoRefLabel, busiRefLabel, groupsLabel;
    private JBLabel uidLabel,
            moduleLabel, createdByLabel, updatedByLabel, createdAtLabel;
    private JBLabel updatedAtLabel;

    private JButton saveButton;

    private TestCase currentTestCase;
    private boolean isEditing = false;

    public TestCaseDetailsPanel() {
        mainPanel = new JBPanel<>(new BorderLayout());
        tabbedPane = new JBTabbedPane();

        detailTab = new JBPanel<>(new GridBagLayout());
        historyTab = new JBPanel<>(new BorderLayout());
        bugTab = new JBPanel<>(new BorderLayout());

        tabbedPane.addTab("Details", detailTab);
        tabbedPane.addTab("History", historyTab);
        tabbedPane.addTab("Open Bugs", bugTab);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancelEdit");
        mainPanel.getActionMap().put("cancelEdit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isEditing) {
                    toggleEditMode(false);
                }
            }
        });

        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Z"), "undo");
        mainPanel.getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //ActionHistory.undo();
                System.out.println("[UNDO] Ctrl+Z triggered");
            }
        });

        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Y"), "redo");
        mainPanel.getActionMap().put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //ActionHistory.redo();
                System.out.println("[REDO] Ctrl+Y triggered");
            }
        });
    }

    public void update(TestCase testCase) {
        this.currentTestCase = testCase;
        toggleEditMode(false);
        loadHistoryAndBugs();
    }

    public void toggleEditMode(boolean editable) {
        isEditing = editable;
        detailTab.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(8, 16);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        if (editable) {
            titleField = new JBTextField(currentTestCase.getTitle());
            expectedArea = new JBTextField(currentTestCase.getExpectedResult());
            stepsArea = new JBTextField(currentTestCase.getSteps());
            priorityField = new JBTextField(currentTestCase.getPriority().getDescription());
            autoRefField = new JBTextField(currentTestCase.getAutomationRef());
            busiRefField = new JBTextField(currentTestCase.getBusinessRef());
            groupsField = new JBTextField(currentTestCase.getGroups() != null ? currentTestCase.getGroups().toString() : "");

            addRow("Title:", titleField, detailTab, gbc, row++);
            addRow("Expected Result:", expectedArea, detailTab, gbc, row++);
            addRow("Steps:", stepsArea, detailTab, gbc, row++);
            addRow("Priority:", priorityField, detailTab, gbc, row++);
            addRow("Automation Ref:", autoRefField, detailTab, gbc, row++);
            addRow("Business Ref:", busiRefField, detailTab, gbc, row++);
            addRow("Groups:", groupsField, detailTab, gbc, row++);
        } else {
            idLabel = createValueLabel(currentTestCase.getId());
            titleLabel = createValueLabel(currentTestCase.getTitle());
            expectedLabel = createValueLabel(currentTestCase.getExpectedResult());
            stepsLabel = createValueLabel(currentTestCase.getSteps());
            priorityLabel = createValueLabel(currentTestCase.getPriority().getDescription());
            autoRefLabel = createValueLabel(currentTestCase.getAutomationRef());
            busiRefLabel = createValueLabel(currentTestCase.getBusinessRef());
            groupsLabel = createValueLabel(currentTestCase.getGroups() != null ? currentTestCase.getGroups().toString() : "");

            addRow("ID:", idLabel, detailTab, gbc, row++);
            addRow("Title:", titleLabel, detailTab, gbc, row++);
            addRow("Expected Result:", expectedLabel, detailTab, gbc, row++);
            addRow("Steps:", stepsLabel, detailTab, gbc, row++);
            addRow("Priority:", priorityLabel, detailTab, gbc, row++);
            addRow("Automation Ref:", autoRefLabel, detailTab, gbc, row++);
            addRow("Business Ref:", busiRefLabel, detailTab, gbc, row++);
            addRow("Groups:", groupsLabel, detailTab, gbc, row++);
        }

        uidLabel = createValueLabel(String.valueOf(currentTestCase.getUid()));
        moduleLabel = createValueLabel(currentTestCase.getModule());
        createdByLabel = createValueLabel(currentTestCase.getCreateBy());
        updatedByLabel = createValueLabel(currentTestCase.getUpdateBy());
        createdAtLabel = createValueLabel(currentTestCase.getCreateAt() != null ? currentTestCase.getCreateAt().toString() : "-");
        updatedAtLabel = createValueLabel(currentTestCase.getUpdateAt() != null ? currentTestCase.getUpdateAt().toString() : "-");

        addRow("UID:", uidLabel, detailTab, gbc, row++);
        addRow("Module:", moduleLabel, detailTab, gbc, row++);
        addRow("Created By:", createdByLabel, detailTab, gbc, row++);
        addRow("Updated By:", updatedByLabel, detailTab, gbc, row++);
        addRow("Created At:", createdAtLabel, detailTab, gbc, row++);
        addRow("Updated At:", updatedAtLabel, detailTab, gbc, row++);

        if (editable) {
            saveButton = new JButton("Save");
            saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
            saveButton.setVisible(true);
            saveButton.addActionListener(e -> onSave());
            saveButton.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "save");
            saveButton.getActionMap().put("save", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onSave();
                }
            });
            gbc.gridx = 1;
            gbc.gridy = row;
            gbc.anchor = GridBagConstraints.EAST;
            detailTab.add(saveButton, gbc);
            SwingUtilities.invokeLater(() -> titleField.requestFocusInWindow());
        } else if (saveButton != null) {
            saveButton.setVisible(false);
        }

        detailTab.revalidate();
        detailTab.repaint();
    }

    private void onSave() {
        String oldTitle = currentTestCase.getTitle();
        String oldExpected = currentTestCase.getExpectedResult();
        String oldSteps = currentTestCase.getSteps();
        Priority oldPriority = currentTestCase.getPriority();

        String newTitle = titleField.getText().trim();
        String newExpected = expectedArea.getText().trim();
        String newSteps = stepsArea.getText().trim();
        String newPriority = priorityField.getText().trim();

        currentTestCase.setTitle(newTitle);
        currentTestCase.setExpectedResult(newExpected);
        currentTestCase.setSteps(newSteps);
        currentTestCase.setPriority(Priority.valueOf(newPriority));

        toggleEditMode(false);
        JOptionPane.showMessageDialog(mainPanel, "✅ Test case saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadHistoryAndBugs() {
        historyTab.removeAll();
        bugTab.removeAll();

        DefaultListModel<String> model = new DefaultListModel<>();
        for (TestCaseHistory history : DB.loadTestCaseHistory()) {
            model.addElement(history.getTimestamp() + " - " + history.getChangeSummary());
        }
        JBList<String> historyList = new JBList<>(model);
        historyList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTab.add(new JBScrollPane(historyList), BorderLayout.CENTER);

        bugTab.add(new JBLabel("🐞 Bug list will be shown here."), BorderLayout.NORTH);
    }

    private JBLabel createValueLabel(String text) {
        JBLabel label = new JBLabel(text != null ? text : "3");  // Use "3" for null values
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return label;
    }

    private void addRow(String label, JComponent input, JBPanel<?> panel, GridBagConstraints gbc, int row) {
        JBLabel keyLabel = new JBLabel(label);
        keyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        keyLabel.setHorizontalAlignment(SwingConstants.LEFT);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(keyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(input, gbc);
    }

    public JBPanel<?> getPanel() {
        return mainPanel;
    }

    public JBPanel<?> getDetailsPanel() {
        return detailTab;
    }

    public JBPanel<?> getHistoryPanel() {
        return historyTab;
    }

    public JBPanel<?> getBugPanel() {
        return bugTab;
    }
}
