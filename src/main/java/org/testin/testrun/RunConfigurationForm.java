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

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunConfiguration;
import org.testin.ui.framework.ChoiceInput;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogComponent;
import org.testin.ui.framework.TextArea;
import org.testin.ui.framework.TextInput;
import org.testin.ui.framework.TextValue;
import org.testin.util.Bundle;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RunConfigurationForm implements DialogComponent {
    private static final boolean EXPANDED = true;
    private static final int CHANGE_LOG_ROWS = 3;

    private final @NotNull JBPanel<?> wrapper;
    private final @NotNull ComponentDialogBase<TextInput> runName;
    private final @NotNull Map<TestRunConfiguration, ComponentDialogBase<? extends DialogComponent>> fields = new EnumMap<>(TestRunConfiguration.class);
    private final @NotNull List<ComponentDialogBase<? extends DialogComponent>> order = new ArrayList<>();

    // UC-TREE-PANEL-021, UC-TREE-PANEL-022
    public RunConfigurationForm(final @NotNull String name, final @NotNull Map<TestRunConfiguration, String> answers) {
        runName = ComponentDialogBase.textField()
                .caption(Bundle.message("run.form.name.caption"))
                .placeholder(Bundle.message("run.form.name.hint"))
                .value(name)
                .build();
        order.add(runName);

        add(TestRunConfiguration.CHANGE_LOG, ComponentDialogBase.textArea()
                .caption(TestRunConfiguration.CHANGE_LOG.getDisplayName())
                .placeholder(Bundle.message("run.form.change.log.hint"))
                .value(answers.getOrDefault(TestRunConfiguration.CHANGE_LOG, ""))
                .rows(CHANGE_LOG_ROWS)
                .build());

        add(TestRunConfiguration.COMMIT_ID, ComponentDialogBase.textField()
                .caption(TestRunConfiguration.COMMIT_ID.getDisplayName())
                .placeholder(Bundle.message("run.form.commit.hint"))
                .value(answers.getOrDefault(TestRunConfiguration.COMMIT_ID, ""))
                .build());

        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (!field.isChoice()) continue;

            final @NotNull ComponentDialogBase<ChoiceInput> choice = ComponentDialogBase.choice(
                    field.getDisplayName(), List.of(field.getOptions()), answers.getOrDefault(field, ""));
            choice.getComponent().onChange(this::applyVisibility);
            add(field, choice);
        }

        wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(CollapsiblePanel.build(Bundle.message("run.form.section"), stacked(), EXPANDED), BorderLayout.CENTER);

        applyVisibility();
    }

    private static @NotNull String textIn(final @NotNull DialogComponent component) {
        return switch (component) {
            case TextValue typed -> typed.getText().trim();
            case TextArea area -> area.getText().trim();
            case ChoiceInput picked -> picked.getValue().trim();
            default -> "";
        };
    }

    private void add(final @NotNull TestRunConfiguration field, final @NotNull ComponentDialogBase<? extends DialogComponent> component) {
        fields.put(field, component);
        order.add(component);
    }

    private @NotNull JBPanel<?> stacked() {
        final @NotNull JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(10)
        ));

        for (final ComponentDialogBase<? extends DialogComponent> component : order) {
            final @NotNull JComponent shown = component.getComponent().getPanel();
            shown.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            panel.add(shown);
        }

        return panel;
    }

    // Rule-TREE-PANEL-120
    private void applyVisibility() {
        fields.forEach((field, component) -> component.getComponent().getPanel().setVisible(field.isShownFor(this::chosenIn)));

        wrapper.revalidate();
        wrapper.repaint();
    }

    private @NotNull String chosenIn(final @NotNull TestRunConfiguration field) {
        return Optional.ofNullable(fields.get(field)).map(component -> textIn(component.getComponent())).orElse("");
    }

    public @NotNull Map<TestRunConfiguration, String> configuration() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);

        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            answers.put(field, answerTo(field));
        }

        return answers;
    }

    public @NotNull String getRunName() {
        return runName.getComponent().getText().trim();
    }

    private @NotNull String answerTo(final @NotNull TestRunConfiguration field) {
        if (!field.isShownFor(this::chosenIn)) return "";

        return chosenIn(field);
    }

    @Override
    public @NotNull JComponent getPanel() {
        return wrapper;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return fields.get(TestRunConfiguration.CHANGE_LOG).getComponent().getFocusComponent();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
