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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum CodeAgent {
    EMPTY(
            Bundle.message("agent.none"),
            "",
            "",
            "",
            ""
    ),

    PI(
            "pi",
            "pi",
            "-p --no-tools --no-session --no-context-files",
            "auth",
            ""
    ),

    CLAUDE_CODE(
            "Claude Code",
            "claude",
            "-p",
            "--version",
            "ANTHROPIC_API_KEY"
    ),

    CODEX(
            "Codex",
            "codex",
            "exec",
            "--version",
            "OPENAI_API_KEY"
    ),

    GEMINI_CLI(
            "Gemini CLI",
            "gemini",
            "-p",
            "--version",
            "GEMINI_API_KEY"
    ),

    OTHER(
            Bundle.message("agent.other"),
            "",
            "",
            "--version",
            ""
    );

    public static final @NotNull String PROMPT_PLACEHOLDER = "{prompt}";

    private final @NotNull String label;

    private final @NotNull String command;

    private final @NotNull String arguments;

    private final @NotNull String check;

    private final @NotNull String keyVariable;

    // UC-CODEGEN-021, Rule-CODEGEN-083
    public static @NotNull CodeAgent named(final @NotNull String stored) {
        return Arrays.stream(values()).filter(agent -> agent.name().equals(stored)).findFirst().orElse(EMPTY);
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    public static @NotNull List<CodeAgent> offered() {
        return Arrays.stream(values()).filter(agent -> agent != EMPTY).toList();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-083
    public boolean isTypedByHand() {
        return this == OTHER;
    }

    @Override
    public @NotNull String toString() {
        return label;
    }
}
