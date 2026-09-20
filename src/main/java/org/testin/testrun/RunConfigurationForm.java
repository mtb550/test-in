/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.testrun;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunConfiguration;
import org.testin.ui.framework.DialogComponent;
import org.testin.util.Bundle;

import java.util.Optional;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class RunConfigurationForm implements DialogComponent {
    private static final boolean EXPANDED = true;

    private final @NotNull JBPanel<?> wrapper;
    private final @NotNull JBTextArea changeLog;
    private final @NotNull JBTextField commitIdField;

    private final @NotNull JBTextField runNameField;
    private final @NotNull Map<TestRunConfiguration, JComponent> fieldMap = new EnumMap<>(TestRunConfiguration.class);

    @Getter(AccessLevel.NONE)
    private final @NotNull Map<TestRunConfiguration, JBLabel> labelMap = new EnumMap<>(TestRunConfiguration.class);

    public RunConfigurationForm(final @NotNull String runName) {
        changeLog = new JBTextArea();
        commitIdField = new JBTextField();
        runNameField = new JBTextField(runName);

        wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(CollapsiblePanel.build(Bundle.message("run.form.section"), buildConfigurationPanel(), EXPANDED), BorderLayout.CENTER);

        applyVisibility();
    }

    private @NotNull JBPanel<?> buildConfigurationPanel() {
        final @NotNull JBPanel<?> configurationPanel = new JBPanel<>(new GridBagLayout());

        final @NotNull GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.anchor = GridBagConstraints.NORTHWEST;
        labelGbc.insets = JBUI.insets(4, 4, 4, 10);

        final @NotNull GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.gridx = 1;
        fieldGbc.weightx = 1.0;
        fieldGbc.anchor = GridBagConstraints.NORTHWEST;
        fieldGbc.insets = JBUI.insets(4, 0, 4, 4);

        runNameField.setColumns(50);
        runNameField.getEmptyText().setText(Bundle.message("run.form.name.hint"));
        addLabeledRow(configurationPanel, labelGbc, fieldGbc, 0, Bundle.message("run.form.name.caption"), runNameField);

        changeLog.setColumns(50);
        changeLog.setRows(3);
        changeLog.setLineWrap(true);
        changeLog.setWrapStyleWord(true);
        changeLog.getEmptyText().setText(Bundle.message("run.form.change.log.hint"));
        keepTabForNavigation(changeLog);

        commitIdField.setColumns(50);
        commitIdField.getEmptyText().setText(Bundle.message("run.form.commit.hint"));

        final @NotNull JBScrollPane changeLogScroller = new JBScrollPane(changeLog);
        register(TestRunConfiguration.CHANGE_LOG, changeLogScroller,
                addLabeledRow(configurationPanel, labelGbc, fieldGbc, 1, TestRunConfiguration.CHANGE_LOG.getDisplayName(), changeLogScroller));
        register(TestRunConfiguration.COMMIT_ID, commitIdField,
                addLabeledRow(configurationPanel, labelGbc, fieldGbc, 2, TestRunConfiguration.COMMIT_ID.getDisplayName(), commitIdField));

        int row = 3;
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (!field.isChoice()) continue;

            final @NotNull ComboBox<String> comboBox = new ComboBox<>(field.getOptions());
            comboBox.setEditable(true);

            comboBox.addActionListener(event -> applyVisibility());

            register(field, comboBox,
                    addLabeledRow(configurationPanel, labelGbc, fieldGbc, row, field.getDisplayName(), comboBox));
            row++;
        }

        configurationPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(10)
        ));

        return configurationPanel;
    }

    private @NotNull JBLabel addLabeledRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints labelGbc, final @NotNull GridBagConstraints fieldGbc, final int row, final @NotNull String label, final @NotNull JComponent component) {
        final @NotNull GridBagConstraints lc = (GridBagConstraints) labelGbc.clone();
        lc.gridy = row;
        final @NotNull JBLabel labelComp = new JBLabel(label);
        labelComp.setVerticalAlignment(SwingConstants.TOP);
        panel.add(labelComp, lc);

        final @NotNull GridBagConstraints fc = (GridBagConstraints) fieldGbc.clone();
        fc.gridy = row;
        panel.add(component, fc);

        return labelComp;
    }

    private void applyVisibility() {
        fieldMap.forEach((field, component) -> {
            final boolean applies = field.isShownFor(this::chosenIn);

            component.setVisible(applies);
            labelMap.get(field).setVisible(applies);
        });

        wrapper.revalidate();
        wrapper.repaint();
    }

    private static void keepTabForNavigation(final @NotNull JComponent field) {
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Set.of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0)));
        field.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Set.of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
    }

    private void register(final @NotNull TestRunConfiguration field, final @NotNull JComponent component, final @NotNull JBLabel label) {
        fieldMap.put(field, component);
        labelMap.put(field, label);
    }

    private @NotNull String chosenIn(final @NotNull TestRunConfiguration field) {
        return Optional.ofNullable(fieldMap.get(field)).map(RunConfigurationForm::textIn).orElse("");
    }

    private static @NotNull String textIn(final @NotNull JComponent component) {
        return switch (component) {
            case JTextComponent typed -> typed.getText().trim();

            case JScrollPane scroller when scroller.getViewport().getView() instanceof JComponent inner ->
                    textIn(inner);
            case ComboBox<?> picked ->
                    Optional.ofNullable(picked.getSelectedItem()).map(Object::toString).map(String::trim).orElse("");
            default -> "";
        };
    }

    public @NotNull Map<TestRunConfiguration, String> configuration() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);

        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            answers.put(field, getFieldValue(field));
        }

        return answers;
    }

    public @NotNull String getRunName() {
        return runNameField.getText().trim();
    }

    public void fillFrom(final @NotNull Map<TestRunConfiguration, String> answers) {
        answers.forEach((field, value) -> Optional.ofNullable(fieldMap.get(field)).ifPresent(component -> textInto(component, value)));

        applyVisibility();
    }

    private static void textInto(final @NotNull JComponent component, final @NotNull String value) {
        switch (component) {
            case JTextComponent typed -> typed.setText(value);

            case JScrollPane scroller when scroller.getViewport().getView() instanceof JComponent inner ->
                    textInto(inner, value);

            case ComboBox<?> picked -> picked.setSelectedItem(value);

            default -> {
            }
        }
    }

    public @NotNull String getFieldValue(final @NotNull TestRunConfiguration field) {
        if (!field.isShownFor(this::chosenIn)) return "";

        return chosenIn(field);
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
    }
}
