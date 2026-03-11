package testGit.viewPanel;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import testGit.pojo.DB;
import testGit.pojo.Priority;
import testGit.pojo.TestCase;
import testGit.pojo.TestCaseHistory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TestCaseDetailsPanel {
    @Getter
    private final JBPanel<?> panel;
    private final JBTabbedPane tabbedPane;
    @Getter
    private final JBPanel<?> detailsTab;
    @Getter
    private final JBPanel<?> historyTab;
    @Getter
    private final JBPanel<?> bugTab;

    // Edit Fields
    private JBTextField titleField, expectedArea, stepsArea, priorityField;
    private JBTextField autoRefField, busiRefField, groupsField;

    private JButton saveButton;
    @Getter
    private TestCase currentTestCase;
    private boolean isEditing = false;

    public TestCaseDetailsPanel() {
        panel = new JBPanel<>(new BorderLayout());
        tabbedPane = new JBTabbedPane();

        detailsTab = new JBPanel<>(new GridBagLayout());
        historyTab = new JBPanel<>(new BorderLayout());
        bugTab = new JBPanel<>(new BorderLayout());

        tabbedPane.addTab("Details", detailsTab);
        tabbedPane.addTab("History", historyTab);
        tabbedPane.addTab("Open Bugs", bugTab);

        panel.add(tabbedPane, BorderLayout.CENTER);

        // Shortcut: Escape to cancel edit
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancelEdit");
        panel.getActionMap().put("cancelEdit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isEditing) toggleEditMode(false);
            }
        });
    }

    /**
     * Entry point for updating the UI. Handles null safely.
     */
    public void update(TestCase testCase) {
        this.currentTestCase = testCase;

        // 1. CLEAR UI IF NULL (Fixes the NPE from reset/escape)
        if (testCase == null) {
            detailsTab.removeAll();
            JBLabel placeholder = new JBLabel("Select a test case to view details");
            placeholder.setForeground(JBColor.GRAY);
            detailsTab.add(placeholder, new GridBagConstraints());
            detailsTab.revalidate();
            detailsTab.repaint();
            return;
        }

        // 2. Otherwise, show read-only details
        toggleEditMode(false);
        loadHistoryAndBugs();
    }

    /**
     * Rebuilds the detail tab based on whether we are viewing or editing.
     */
    public void toggleEditMode(boolean editable) {
        // NULL GUARD: Prevents crashes when resetting or closing
        if (currentTestCase == null) return;

        isEditing = editable;
        detailsTab.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(8, 16);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        if (editable) {
            setupEditMode(gbc, row);
        } else {
            setupViewMode(gbc, row);
        }

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void setupEditMode(GridBagConstraints gbc, int row) {
        titleField = new JBTextField(currentTestCase.getTitle());
        expectedArea = new JBTextField(currentTestCase.getExpected());
        stepsArea = new JBTextField(currentTestCase.getSteps());
        priorityField = new JBTextField(currentTestCase.getPriority() != null ? currentTestCase.getPriority().getDescription() : "");
        autoRefField = new JBTextField(currentTestCase.getAutoRef());
        busiRefField = new JBTextField(currentTestCase.getBusiRef());
        groupsField = new JBTextField(currentTestCase.getGroups() != null ? currentTestCase.getGroups().toString() : "");

        addRow("Title:", titleField, detailsTab, gbc, row++);
        addRow("Expected Result:", expectedArea, detailsTab, gbc, row++);
        addRow("Steps:", stepsArea, detailsTab, gbc, row++);
        addRow("Priority:", priorityField, detailsTab, gbc, row++);
        addRow("Automation Ref:", autoRefField, detailsTab, gbc, row++);
        addRow("Business Ref:", busiRefField, detailsTab, gbc, row++);
        addRow("Groups:", groupsField, detailsTab, gbc, row++);

        addCommonMetaRows(gbc, row);

        saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> onSave());
        gbc.gridx = 1;
        gbc.gridy = row + 10;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        detailsTab.add(saveButton, gbc);

        SwingUtilities.invokeLater(() -> titleField.requestFocusInWindow());
    }

    private void setupViewMode(GridBagConstraints gbc, int row) {
        addRow("ID:", createValueLabel(currentTestCase.getId()), detailsTab, gbc, row++);
        addRow("Title:", createValueLabel(currentTestCase.getTitle()), detailsTab, gbc, row++);
        addRow("Expected Result:", createValueLabel(currentTestCase.getExpected()), detailsTab, gbc, row++);
        addRow("Steps:", createValueLabel(currentTestCase.getSteps()), detailsTab, gbc, row++);
        addRow("Priority:", createValueLabel(currentTestCase.getPriority() != null ? currentTestCase.getPriority().getDescription() : "-"), detailsTab, gbc, row++);
        addRow("Automation Ref:", createValueLabel(currentTestCase.getAutoRef()), detailsTab, gbc, row++);
        addRow("Business Ref:", createValueLabel(currentTestCase.getBusiRef()), detailsTab, gbc, row++);
        addRow("Groups:", createValueLabel(currentTestCase.getGroups() != null ? currentTestCase.getGroups().toString() : "-"), detailsTab, gbc, row++);

        addCommonMetaRows(gbc, row);
    }

    private void addCommonMetaRows(GridBagConstraints gbc, int row) {
        addRow("UID:", createValueLabel(String.valueOf(currentTestCase.getUid())), detailsTab, gbc, row++);
        addRow("Module:", createValueLabel(currentTestCase.getModule()), detailsTab, gbc, row++);
        addRow("Created By:", createValueLabel(currentTestCase.getCreateBy()), detailsTab, gbc, row++);
        addRow("Updated By:", createValueLabel(currentTestCase.getUpdateBy()), detailsTab, gbc, row++);
        addRow("Created At:", createValueLabel(currentTestCase.getCreateAt() != null ? currentTestCase.getCreateAt().toString() : "-"), detailsTab, gbc, row++);
        addRow("Updated At:", createValueLabel(currentTestCase.getUpdateAt() != null ? currentTestCase.getUpdateAt().toString() : "-"), detailsTab, gbc, row++);
    }

    private void onSave() {
        if (currentTestCase == null) return;

        currentTestCase.setTitle(titleField.getText().trim());
        currentTestCase.setExpected(expectedArea.getText().trim());
        currentTestCase.setSteps(stepsArea.getText().trim());
        try {
            currentTestCase.setPriority(Priority.valueOf(priorityField.getText().toUpperCase()));
        } catch (Exception ignored) {
        }

        toggleEditMode(false);
        // Better than JOptionPane: uses IntelliJ's notification system if available
        System.out.println("Saved: " + currentTestCase.getId());
    }

    private void loadHistoryAndBugs() {
        historyTab.removeAll();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (TestCaseHistory h : DB.loadTestCaseHistory()) {
            model.addElement(h.getTimestamp() + " - " + h.getChangeSummary());
        }
        JBList<String> list = new JBList<>(model);
        historyTab.add(new JBScrollPane(list), BorderLayout.CENTER);

        bugTab.removeAll();
        bugTab.add(new JBLabel("No bugs found for this test case."), BorderLayout.NORTH);
    }

    private JBLabel createValueLabel(String text) {
        JBLabel label = new JBLabel((text == null || text.isEmpty()) ? "-" : text);
        label.setFont(JBUI.Fonts.label(14));
        return label;
    }

    private void addRow(String label, JComponent input, JBPanel<?> panel, GridBagConstraints gbc, int row) {
        JBLabel keyLabel = new JBLabel(label);
        keyLabel.setFont(JBUI.Fonts.label(14).asBold());

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

}