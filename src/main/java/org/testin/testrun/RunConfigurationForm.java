package org.testin.testrun;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunConfiguration;
import org.testin.ui.framework.DialogComponent;

import java.util.Optional;
import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * The run-configuration part of the create-run dialog: the (fixed) run name,
 * change log, commit id, and one editable combo per configured field —
 * collapsible, as before. A framework dialog component; the selection tree is
 * a separate component.
 */
@Getter
public class RunConfigurationForm implements DialogComponent {

    private final @NotNull JBPanel<?> wrapper;
    private final @NotNull JBTextField changeLog;
    private final @NotNull JBTextField commitIdField;
    private final @NotNull Map<TestRunConfiguration, JComponent> fieldMap = new EnumMap<>(TestRunConfiguration.class);

    public RunConfigurationForm(final @NotNull String runName) {
        changeLog = new JBTextField();
        commitIdField = new JBTextField();

        wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(CollapsiblePanel.build("Configuration details", buildConfigurationPanel(runName), false), BorderLayout.CENTER);
    }

    private @NotNull JBPanel<?> buildConfigurationPanel(final @NotNull String runName) {
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
            if (field.isChoice()) {
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

    private void addLabeledRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints labelGbc, final @NotNull GridBagConstraints fieldGbc, final int row, final @NotNull String label, final @NotNull JComponent component) {
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
        // Nothing under that field, a field that is not a combo box, and a combo
        // box with nothing chosen all mean the same thing here: no value.
        return Optional.ofNullable(fieldMap.get(field))
                .filter(ComboBox.class::isInstance)
                .map(comp -> ((ComboBox<?>) comp).getSelectedItem())
                .map(selected -> selected.toString().trim())
                .orElse("");
    }

    @Override
    public @NotNull JComponent getPanel() {
        return wrapper;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return changeLog;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Form fields have no submit gesture of their own; the declared keys confirm.
    }
}
