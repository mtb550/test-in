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
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Shortcuts;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WriteBodies {
    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-091
    public static void forAll(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull Optional<TestinEditor> editor) {
        final @NotNull AgentConnection connection = AgentConnection.stored();
        if (!connection.isConnected() || testCases.isEmpty()) return;

        final @NotNull List<TestCaseDto> theirs = testCases.stream().filter(tc -> CodeNavigation.available().hasTheWrittenBody(p, tc)).toList();
        final @NotNull List<TestCaseDto> empty = testCases.stream().filter(tc -> !theirs.contains(tc)).toList();

        if (theirs.isEmpty()) {
            ask(p, connection, empty, List.of(), editor);
            return;
        }

        // Rule-CODEGEN-091
        new ConfirmDialog(p, Bundle.message("agent.over.title"),
                Bundle.message("agent.over.message", String.valueOf(theirs.size())), "", "",
                Bundle.message("agent.over.confirm"),
                () -> ask(p, connection, empty, theirs, editor),
                List.of(new ConfirmDialog.Alternative(Shortcuts.ConfirmAlternative, Bundle.message("agent.over.skip"),
                        () -> ask(p, connection, empty, List.of(), editor)))).show();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-089
    private static void ask(final @NotNull Project p, final @NotNull AgentConnection connection, final @NotNull List<TestCaseDto> empty, final @NotNull List<TestCaseDto> theirs, final @NotNull Optional<TestinEditor> editor) {
        if (empty.isEmpty() && theirs.isEmpty()) return;

        final @NotNull AgentTranscript transcript = new AgentTranscript();
        final int asked = empty.size() + theirs.size();

        BackgroundWork.run(p, Bundle.message("agent.task.title"), Bundle.message("agent.task.failed"), true,
                indicator -> written(p, connection, empty, theirs, transcript, indicator),
                written -> sayWhatTheAgentDid(p, written, asked, transcript),
                () -> editor.ifPresent(TestinEditor::refreshView));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-088
    private static int written(final @NotNull Project p, final @NotNull AgentConnection connection, final @NotNull List<TestCaseDto> empty, final @NotNull List<TestCaseDto> theirs, final @NotNull AgentTranscript transcript, final @NotNull ProgressIndicator indicator) {
        final @NotNull AgentCli agent = AgentCli.onPath(indicator);
        final @NotNull List<TestCaseDto> asked = Stream.concat(empty.stream(), theirs.stream()).toList();
        int written = 0;

        indicator.setIndeterminate(false);
        for (final TestCaseDto tc : asked) {
            if (indicator.isCanceled()) return written;

            indicator.setText2(tc.getDescription());
            indicator.setFraction((double) written / asked.size());

            if (bodyLanded(p, agent, connection, tc, transcript, theirs.contains(tc))) written++;
        }

        return written;
    }

    // UC-CODEGEN-003, Rule-CODEGEN-090, Rule-CODEGEN-091
    private static boolean bodyLanded(final @NotNull Project p, final @NotNull AgentCli agent, final @NotNull AgentConnection connection, final @NotNull TestCaseDto tc, final @NotNull AgentTranscript transcript, final boolean overWhatIsThere) {
        final @NotNull String prompt = BodyPrompt.of(connection.promptTemplate(), tc, Fqcn.methodNameOf(tc));
        final @NotNull Optional<String> said = agent.ask(connection, prompt);
        final @NotNull Optional<String> statements = said.flatMap(AgentAnswer::statementsIn);

        final boolean landed = statements.filter(written -> overWhatIsThere
                ? CodeNavigation.available().replaceBody(p, tc, written)
                : CodeNavigation.available().fillBody(p, tc, written)).isPresent();

        transcript.record(tc, prompt, said.orElse(Bundle.message("agent.said.nothing")), outcomeOf(landed, said, statements));

        return landed;
    }

    // Rule-CODEGEN-090
    private static @NotNull String outcomeOf(final boolean landed, final @NotNull Optional<String> said, final @NotNull Optional<String> statements) {
        if (landed) return Bundle.message("agent.said.written");
        if (said.isEmpty()) return Bundle.message("agent.said.no.answer");
        if (statements.isEmpty()) return Bundle.message("agent.said.not.java");

        return Bundle.message("agent.said.no.method");
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
