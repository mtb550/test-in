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

package org.testin.setting;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.AgentCli;
import org.testin.codegen.AgentConnection;
import org.testin.codegen.CodeAgent;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Fonts;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.time.Duration;
import java.util.Optional;

// UC-CODEGEN-021, Rule-CODEGEN-083
public final class AgentSettingsConfigurable implements SearchableConfigurable {
    static final @NotNull String ID = "org.testin.setting.AgentSettingsConfigurable";

    private static final @NotNull Duration CHECK_TIMEOUT = Duration.ofSeconds(20);

    private final @NotNull ComboBox<CodeAgent> agentBox = new ComboBox<>(CodeAgent.offered().toArray(CodeAgent[]::new));
    private final @NotNull JBTextField commandField = new JBTextField();
    private final @NotNull JBTextField argumentsField = new JBTextField();
    private final @NotNull JBTextField checkField = new JBTextField();
    private final @NotNull JBTextField keyVariableField = new JBTextField();
    private final @NotNull JBLabel said = new JBLabel("");

    private static @NotNull JBLabel hint(final @NotNull String text) {
        final @NotNull JBLabel note = new JBLabel(text);

        note.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        note.setFont(Fonts.small());

        return note;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return Bundle.message("agent.page.name");
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    @Override
    public @NotNull JComponent createComponent() {
        agentBox.addActionListener(_ -> fillFromTheChosenAgent());

        final @NotNull JButton check = new JButton(Bundle.message("agent.check.button"));
        check.addActionListener(_ -> said.setText(whatTheAgentSaid()));

        final @NotNull JBPanel<?> buttons = new JBPanel<>();
        buttons.setOpaque(false);
        buttons.add(check);

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.agent")), agentBox, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.command")), commandField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.arguments")), argumentsField, 1, false)
                .addComponentToRightColumn(hint(Bundle.message("agent.hint.arguments")))
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.check")), checkField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel(Bundle.message("agent.label.key.variable")), keyVariableField, 1, false)
                .addComponentToRightColumn(hint(Bundle.message("agent.hint.key.variable")))
                .addVerticalGap(10)
                .addComponent(hint(Bundle.message("agent.keys")))
                .addComponent(said)
                .addComponent(buttons)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    private void fillFromTheChosenAgent() {
        final @NotNull CodeAgent chosen = chosenAgent();
        if (chosen.isTypedByHand()) return;

        commandField.setText(chosen.getCommand());
        argumentsField.setText(chosen.getArguments());
        checkField.setText(chosen.getCheck());
        keyVariableField.setText(chosen.getKeyVariable());
    }

    private @NotNull CodeAgent chosenAgent() {
        return Optional.ofNullable(agentBox.getItem()).orElse(CodeAgent.OTHER);
    }

    // UC-CODEGEN-021, Rule-CODEGEN-085, Rule-CODEGEN-087
    private @NotNull String whatTheAgentSaid() {
        final @NotNull AgentConnection typed = typedConnection();
        if (!typed.isConnected()) return Bundle.message("agent.check.no.command");

        final @NotNull Optional<String> answered = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                () -> AgentCli.onPath(ProgressManager.getInstance().getProgressIndicator()).check(typed),
                Bundle.message("agent.check.button"), true, null);
        if (answered.isEmpty()) return Bundle.message("agent.check.not.found", typed.command());

        final @NotNull String keys = typed.keyIsSet()
                .map(set -> set ? Bundle.message("agent.key.set", typed.keyVariable()) : Bundle.message("agent.key.not.set", typed.keyVariable()))
                .map(said -> " " + said)
                .orElse("");

        return answered.orElseThrow().isBlank()
                ? Bundle.message("agent.check.said.nothing", typed.command()) + keys
                : Bundle.message("agent.check.answered", firstLineOf(answered.orElseThrow())) + keys;
    }

    private @NotNull String firstLineOf(final @NotNull String answered) {
        return answered.lines().findFirst().orElse(answered).strip();
    }

    private @NotNull AgentConnection typedConnection() {
        return new AgentConnection(commandField.getText().trim(), argumentsField.getText().trim(),
                checkField.getText().trim(), keyVariableField.getText().trim(), "", CHECK_TIMEOUT);
    }

    @Override
    public boolean isModified() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        return !settings.agentName.equals(chosenAgent().name())
                || !settings.agentCommand.equals(commandField.getText().trim())
                || !settings.agentArguments.equals(argumentsField.getText().trim())
                || !settings.agentCheck.equals(checkField.getText().trim())
                || !settings.agentKeyVariable.equals(keyVariableField.getText().trim());
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    @Override
    public void apply() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        settings.agentName = chosenAgent().name();
        settings.agentCommand = commandField.getText().trim();
        settings.agentArguments = argumentsField.getText().trim();
        settings.agentCheck = checkField.getText().trim();
        settings.agentKeyVariable = keyVariableField.getText().trim();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    @Override
    public void reset() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        agentBox.setItem(CodeAgent.named(settings.agentName) == CodeAgent.EMPTY ? CodeAgent.OTHER : CodeAgent.named(settings.agentName));
        commandField.setText(settings.agentCommand);
        argumentsField.setText(settings.agentArguments);
        checkField.setText(settings.agentCheck);
        keyVariableField.setText(settings.agentKeyVariable);
        said.setText("");
    }
}
