package testGit.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.GroupType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AddNewTestCaseDialog extends DialogWrapper {
    private final JBTextField titleField = new JBTextField();
    private final JBLabel charCounter = new JBLabel("0 / 100");
    private final ComboBox<String> priorityCombo = new ComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});

    // Using standard JBCheckBoxes in a panel for Groups to avoid 'CheckedComboBox' resolution issues
    private final JBCheckBox regressionBox = new JBCheckBox(GroupType.Regression.name());
    private final JBCheckBox sanityBox = new JBCheckBox(GroupType.Sanity.name());
    private final JBCheckBox smokeBox = new JBCheckBox(GroupType.Smoke.name());
    private final JBCheckBox securityBox = new JBCheckBox(GroupType.Security.name());
    private final JBCheckBox functionalBox = new JBCheckBox(GroupType.Functional.name());
    private final JBCheckBox validationBox = new JBCheckBox(GroupType.Validation.name());
    private final JBCheckBox uiBox = new JBCheckBox(GroupType.UI.name());

    public AddNewTestCaseDialog() {
        super(true);
        setTitle("New Test Case");
        setOKButtonText("Create");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        // Set width to 600px (approx 30% of standard screen)
        panel.setPreferredSize(new Dimension(600, 200));
        panel.setBorder(JBUI.Borders.empty(12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);

        // --- Row 0: Title Label and Counter ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JBLabel("Title:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        charCounter.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
        charCounter.setForeground(UIUtil.getContextHelpForeground());
        panel.add(charCounter, gbc);

        // --- Row 1: Title Field ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        titleField.getEmptyText().setText("Max 100 characters...");
        panel.add(titleField, gbc);

        // --- Row 2: Priority ---
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JBLabel("Priority:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(priorityCombo, gbc);

        // --- Row 3: Groups ---
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        panel.add(new JBLabel("Groups:"), gbc);

        JBPanel<?> groupsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        groupsPanel.add(regressionBox);
        groupsPanel.add(sanityBox);
        groupsPanel.add(smokeBox);
        groupsPanel.add(functionalBox);
        groupsPanel.add(securityBox);
        groupsPanel.add(validationBox);
        groupsPanel.add(uiBox);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(groupsPanel, gbc);

        // Live counter logic with theme-aware colors
        titleField.addCaretListener(e -> {
            int len = titleField.getText().length();
            charCounter.setText(len + " / 100");
            charCounter.setForeground(len > 100 ? JBColor.RED : UIUtil.getContextHelpForeground());
        });

        return panel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String text = titleField.getText().trim();
        if (text.isEmpty()) return new ValidationInfo("Title cannot be empty", titleField);
        if (text.length() > 100) return new ValidationInfo("Title is too long (max 100)", titleField);
        return null;
    }

    public String getTitle() {
        return titleField.getText().trim();
    }

    public String getPriority() {
        return (String) priorityCombo.getSelectedItem();
    }

    public List<GroupType> getSelectedGroups() {
        List<GroupType> groups = new ArrayList<>();
        if (regressionBox.isSelected()) groups.add(GroupType.Regression);
        if (sanityBox.isSelected()) groups.add(GroupType.Sanity);
        if (smokeBox.isSelected()) groups.add(GroupType.Smoke);
        if (functionalBox.isSelected()) groups.add(GroupType.Functional);
        if (validationBox.isSelected()) groups.add(GroupType.Validation);
        if (securityBox.isSelected()) groups.add(GroupType.Security);
        if (uiBox.isSelected()) groups.add(GroupType.UI);
        return groups;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return titleField;
    }
}