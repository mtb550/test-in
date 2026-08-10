package org.testin.testRun;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestRunConfiguration;
import org.testin.mappers.TestRunItems;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class CreateTestRunDialog {

    private final JBPanel<?> mainPanel;
    private final CheckboxTree tree;

    private final JBTextField changeLog;
    private final JBTextField commitIdField;
    private final Map<TestRunConfiguration, JComponent> fieldMap = new EnumMap<>(TestRunConfiguration.class);

    public CreateTestRunDialog(final @NotNull String runName, final CheckedTreeNode root, final @NotNull Map<@NotNull UUID, @NotNull TestRunItems> resultsMap) {
        mainPanel = new JBPanel<>(new BorderLayout());
        changeLog = new JBTextField();
        commitIdField = new JBTextField();

        final JBPanel<?> configurationPanel = buildConfigurationPanel(runName);
        mainPanel.add(CollapsiblePanelImpl.build("Configuration details", configurationPanel, false), BorderLayout.NORTH);

        tree = new CheckboxTree(RunTreeCellRendererImpl.create(resultsMap), root, new CheckboxTreeBase.CheckPolicy(true, true, true, true));
        TreeUtil.expandAll(tree);

        mainPanel.add(new JBScrollPane(tree), BorderLayout.CENTER);

        mainPanel.setPreferredSize(new Dimension(JBUI.scale(900), JBUI.scale(600)));
    }

    private JBPanel<?> buildConfigurationPanel(final @NotNull String runName) {
        final JBPanel<?> configurationPanel = new JBPanel<>(new GridBagLayout());

        final GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.anchor = GridBagConstraints.NORTHWEST;
        labelGbc.insets = JBUI.insets(4, 4, 4, 10);

        final GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.gridx = 1;
        fieldGbc.weightx = 1.0;
        fieldGbc.anchor = GridBagConstraints.NORTHWEST;
        fieldGbc.insets = JBUI.insets(4, 0, 4, 4);

        final JBTextField runNameField = new JBTextField(runName);
        runNameField.setEditable(false);
        runNameField.setEnabled(false);
        runNameField.setColumns(50);
        addLabeledRow(configurationPanel, labelGbc, fieldGbc, 0, "Test Run name:", runNameField);

        changeLog.setColumns(50);
        commitIdField.setColumns(50);
        addLabeledRow(configurationPanel, labelGbc, fieldGbc, 1, TestRunConfiguration.CHANGE_LOG.getDisplayName(), changeLog);
        addLabeledRow(configurationPanel, labelGbc, fieldGbc, 2, TestRunConfiguration.COMMIT_ID.getDisplayName(), commitIdField);

        int row = 3;
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (field.getOptions() != null) {
                final ComboBox<String> comboBox = new ComboBox<>(field.getOptions());
                comboBox.setEditable(true);
                fieldMap.put(field, comboBox);
                addLabeledRow(configurationPanel, labelGbc, fieldGbc, row, field.getDisplayName(), comboBox);
                row++;
            }
        }

        configurationPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(10)
        ));

        return configurationPanel;
    }

    private void addLabeledRow(final JBPanel<?> panel, final GridBagConstraints labelGbc, final GridBagConstraints fieldGbc, final int row, final String label, final JComponent component) {
        final GridBagConstraints lc = (GridBagConstraints) labelGbc.clone();
        lc.gridy = row;
        final JBLabel labelComp = new JBLabel(label);
        labelComp.setVerticalAlignment(SwingConstants.TOP);
        panel.add(labelComp, lc);

        final GridBagConstraints fc = (GridBagConstraints) fieldGbc.clone();
        fc.gridy = row;
        panel.add(component, fc);
    }

    public @NotNull String getFieldValue(final @NotNull TestRunConfiguration field) {
        final JComponent comp = fieldMap.get(field);
        if (comp instanceof ComboBox<?> comboBox) {
            final Object selected = comboBox.getSelectedItem();
            return selected != null ? selected.toString().trim() : "";
        }
        return "";
    }
}