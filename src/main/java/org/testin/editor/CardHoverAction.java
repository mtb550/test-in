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

package org.testin.editor;

import org.testin.codegen.CodeOn;
import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Icons;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.Automated;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.navigate.NavigateToTestCaseAction;
import org.testin.notifications.Notifier;
import org.testin.runner.RunTestCases;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.actions.Declared;
import org.testin.services.OptionalPlugin;
import org.testin.util.Bundle;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum CardHoverAction {
    NAVIGATE_TO_TEST_METHOD(
            Bundle.message("action.Testin.NavigateToCode.text"),
            "Testin.NavigateToCode",
            List.of(OptionalPlugin.JAVA),
            Icons.TEST_CASE,
            (p, cases) -> NavigateToCodeAction.execute(p, cases.getFirst()),
            CardHoverAction::anywhere
    ),

    RUN_TEST_CASE(
            Bundle.message("card.run.test.case"),
            "Testin.RunTestCase",
            List.of(OptionalPlugin.JAVA, OptionalPlugin.TESTNG),
            AllIcons.RunConfigurations.TestState.Run,
            RunTestCases::run,
            CardHoverAction::anywhere
    ),

    STOP_TEST_CASE(
            Bundle.message("card.stop.test.case"),
            "",
            List.of(OptionalPlugin.TESTNG),
            AllIcons.Actions.Suspend,
            CardHoverAction::stopRun,
            CardHoverAction::anywhere
    ),

    NAVIGATE_TO_TEST_CASE(
            Bundle.message("action.Testin.NavigateToTestCase.text"),
            "Testin.NavigateToTestCase",
            List.of(),
            Icons.fieldLetter("tc", Icons.GRAY),
            (p, cases) -> NavigateToTestCaseAction.execute(p, cases.getFirst()),
            NavigateToTestCaseAction::whyNot
    );

    private final @NotNull String tooltip;
    private final @NotNull String actionId;
    @Getter(AccessLevel.NONE)
    private final @NotNull List<OptionalPlugin> requires;

    private final @NotNull Icon icon;

    @Getter(AccessLevel.NONE)
    private final @NotNull BiConsumer<Project, List<TestCaseDto>> onClick;

    @Getter(AccessLevel.NONE)
    private final @NotNull BiFunction<DirectoryDto, TestCaseDto, Optional<String>> whyNotOnCard;

    public record Offered(@NotNull CardHoverAction action, @NotNull Optional<String> whyNot) {
        public boolean works() {
            return whyNot.isEmpty();
        }

        public @NotNull String hintText() {
            return whyNot.orElseGet(action::hint);
        }
    }

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-234, Rule-EDITOR-PANEL-235
    public static @NotNull List<Offered> onCard(final @NotNull Project p, final @NotNull DirectoryDto openOn, final @NotNull TestCaseDto tc) {
        return Stream.of(NAVIGATE_TO_TEST_METHOD, runSlot(p, tc), NAVIGATE_TO_TEST_CASE)
                .map(action -> new Offered(action, action.whyNotOffered(p).or(() -> action.whyNotOnCard.apply(openOn, tc))))
                .toList();
    }

    private static @NotNull Optional<String> anywhere(final @NotNull DirectoryDto openOn, final @NotNull TestCaseDto tc) {
        return Optional.empty();
    }

    // UC-CODEGEN-009, Rule-CODEGEN-005
    public boolean enableOrExplain(final @NotNull Presentation presentation) {
        return requires.stream().allMatch(plugin -> plugin.enableOrExplain(presentation, tooltip));
    }

    // UC-EDITOR-PANEL-047, UC-EDITOR-PANEL-048
    public @NotNull Icon iconOn(final @NotNull Automated automation) {
        return this == NAVIGATE_TO_TEST_METHOD ? automation.getIcon() : icon;
    }

    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        execute(p, List.of(tc));
    }

    public void execute(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        if (cases.isEmpty()) return;

        onClick.accept(p, cases);
    }

    // UC-EDITOR-PANEL-043
    private static void stopRun(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final int stopped = Services.getInstance(p, TestNGExecution.class).stop(cases);

        if (stopped > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, Done.STOPPED, stopped);
    }

    public static @NotNull CardHoverAction runSlot(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return runSlot(p, List.of(tc));
    }

    // UC-EDITOR-PANEL-043
    public static @NotNull CardHoverAction runSlot(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        return cases.stream().anyMatch(tc -> execution.isRunning(tc.getId()))
                ? STOP_TEST_CASE
                : RUN_TEST_CASE;
    }

    public @NotNull String getHintText(final @NotNull Project p) {
        return whyNotOffered(p).orElseGet(this::hint);
    }

    private @NotNull String hint() {
        return (tooltip + " " + Declared.shortcutText(actionId)).trim();
    }

    // UC-EDITOR-PANEL-047, Rule-CODEGEN-082
    public @NotNull Optional<String> whyNotOffered(final @NotNull Project p) {
        final @NotNull Optional<String> missing = requires.stream()
                .filter(plugin -> !plugin.isAvailable())
                .findFirst()
                .map(plugin -> plugin.needs(tooltip));

        // Rule-CODEGEN-082
        if (missing.isPresent() || !requires.contains(OptionalPlugin.JAVA)) return missing;
        return CodeOn.whyOff(p);
    }

    public boolean isOffered(final @NotNull Project p) {
        return whyNotOffered(p).isEmpty();
    }
}
