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

package org.testin.codegen;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.navigate.CodeNavigation;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WriteBodies {
    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-089
    public static void forAll(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull Optional<TestinEditor> editor) {
        final @NotNull AgentConnection connection = AgentConnection.stored();
        if (!connection.isConnected() || testCases.isEmpty()) return;

        final @NotNull AgentTranscript transcript = new AgentTranscript();

        BackgroundWork.run(p, Bundle.message("agent.task.title"), Bundle.message("agent.task.failed"), true,
                indicator -> written(p, connection, testCases, transcript, indicator),
                written -> sayWhatTheAgentDid(p, written, testCases.size(), transcript),
                () -> editor.ifPresent(TestinEditor::refreshView));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-088
    private static int written(final @NotNull Project p, final @NotNull AgentConnection connection, final @NotNull List<TestCaseDto> testCases, final @NotNull AgentTranscript transcript, final @NotNull ProgressIndicator indicator) {
        final @NotNull AgentCli agent = AgentCli.onPath(indicator);
        int written = 0;

        indicator.setIndeterminate(false);
        for (final TestCaseDto tc : testCases) {
            if (indicator.isCanceled()) return written;

            indicator.setText2(tc.getDescription());
            indicator.setFraction((double) written / testCases.size());

            if (bodyLanded(p, agent, connection, tc, transcript)) written++;
        }

        return written;
    }

    // UC-CODEGEN-021, Rule-CODEGEN-088, Rule-CODEGEN-090
    private static boolean bodyLanded(final @NotNull Project p, final @NotNull AgentCli agent, final @NotNull AgentConnection connection, final @NotNull TestCaseDto tc, final @NotNull AgentTranscript transcript) {
        final @NotNull String prompt = BodyPrompt.of(connection.promptTemplate(), tc, Fqcn.methodNameOf(tc));
        final @NotNull Optional<String> said = agent.ask(connection, prompt);

        final boolean landed = said.flatMap(AgentAnswer::statementsIn)
                .filter(statements -> CodeNavigation.available().fillBody(p, tc, statements))
                .isPresent();

        final @NotNull String outcome = landed ? Bundle.message("agent.said.written") : Bundle.message("agent.said.dropped");
        transcript.record(tc, prompt, said.orElse(Bundle.message("agent.said.nothing")), outcome);

        return landed;
    }

    // UC-CODEGEN-021, Rule-CODEGEN-090
    private static void sayWhatTheAgentDid(final @NotNull Project p, final int written, final int asked, final @NotNull AgentTranscript transcript) {
        if (transcript.isEmpty()) return;

        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        notifier.infoWithActions(p, Bundle.message("agent.done.written"),
                Bundle.message("agent.done.detail", String.valueOf(written), String.valueOf(asked)),
                notifier.lastingAction(Bundle.message("agent.said.show"), () -> new AgentSaidDialog(p, transcript.read()).show()));
    }
}
