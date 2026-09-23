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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.codegen.CodeOn;
import org.testin.model.Automated;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.navigate.NavigateToTestCaseAction;
import org.testin.navigate.NavigateToTestMethodAction;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.runner.RunTestCases;
import org.testin.runner.TestNGExecution;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Icons;

import javax.swing.Icon;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum CardHoverAction {
    NAVIGATE_TO_TEST_METHOD(
            Bundle.message("action.Testin.NavigateToTestMethod.text"),
            "Testin.NavigateToTestMethod",
            List.of(OptionalPlugin.JAVA),
            Icons.TEST_CASE,
            (p, testCases) -> NavigateToTestMethodAction.execute(p, testCases.getFirst()),
            _ -> Optional.empty()
    ),

    RUN_TEST_METHOD(
            Bundle.message("action.Testin.RunTestMethod.text"),
            "Testin.RunTestMethod",
            List.of(OptionalPlugin.JAVA, OptionalPlugin.TESTNG),
            AllIcons.RunConfigurations.TestState.Run,
            RunTestCases::run,
            _ -> Optional.empty()
    ),

    STOP_TEST_METHOD(
            Bundle.message("card.stop.test.method"),
            "",
            List.of(OptionalPlugin.TESTNG),
            AllIcons.Actions.Suspend,
            CardHoverAction::stopRun,
            _ -> Optional.empty()
    ),

    NAVIGATE_TO_TEST_CASE(
            Bundle.message("action.Testin.NavigateToTestCase.text"),
            "Testin.NavigateToTestCase",
            List.of(),
            Icons.TEST_CASE_LETTER,
            (p, testCases) -> NavigateToTestCaseAction.execute(p, testCases.getFirst()),
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
    private final @NotNull Function<TestCaseDto, Optional<String>> whyNotOnCard;

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-234, Rule-EDITOR-PANEL-235
    public static @NotNull List<Offered> onCard(final @NotNull Project p, final @NotNull DirectoryDto openOn, final @NotNull TestCaseDto tc) {
        final @NotNull List<CardHoverAction> buttons = openOn.isTestCaseContainer()
                ? List.of(NAVIGATE_TO_TEST_METHOD, RUN_TEST_METHOD)
                : List.of(NAVIGATE_TO_TEST_METHOD, RUN_TEST_METHOD, NAVIGATE_TO_TEST_CASE);

        return buttons.stream()
                .map(button -> button.offer(p, tc))
                .toList();
    }

    // UC-EDITOR-PANEL-043
    private static void stopRun(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final int stopped = Services.getInstance(p, TestNGExecution.class).stop(testCases);

        if (stopped > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, Done.STOPPED, stopped);
    }

    public static @NotNull CardHoverAction runSlot(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return runSlot(p, List.of(tc));
    }

    // UC-EDITOR-PANEL-043
    public static @NotNull CardHoverAction runSlot(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        return testCases.stream().anyMatch(tc -> execution.isRunning(tc.getId()))
                ? STOP_TEST_METHOD
                : RUN_TEST_METHOD;
    }

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-234
    public @NotNull Offered offer(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull CardHoverAction now = gestureOn(p, tc);

        return new Offered(now, now.whyNotOffered(p).or(() -> now.whyNotOnCard.apply(tc)));
    }

    // UC-EDITOR-PANEL-043
    public @NotNull CardHoverAction gestureOn(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return this == RUN_TEST_METHOD ? runSlot(p, tc) : this;
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-180
    public void executeFor(final @NotNull TestinEditor editor, final @NotNull TestCaseDto tc) {
        if (this == RUN_TEST_METHOD) editor.launching(tc.getId());

        execute(editor.getProject(), tc);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-005, Rule-CODEGEN-082
    public boolean enableOrExplain(final @NotNull Project p, final @NotNull Presentation presentation) {
        if (!requires.stream().allMatch(plugin -> plugin.enableOrExplain(presentation, tooltip))) return false;

        return whyNotOffered(p).map(reason -> {
            presentation.setEnabled(false);
            presentation.setDescription(reason);

            return false;
        }).orElse(true);
    }

    // UC-EDITOR-PANEL-047, UC-EDITOR-PANEL-048
    public @NotNull Icon iconOn(final @NotNull Automated automation) {
        return this == NAVIGATE_TO_TEST_METHOD ? automation.getIcon() : icon;
    }

    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        execute(p, List.of(tc));
    }

    public void execute(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;

        onClick.accept(p, testCases);
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

    public record Offered(@NotNull CardHoverAction action, @NotNull Optional<String> whyNot) {
        public boolean works() {
            return whyNot.isEmpty();
        }

        public @NotNull String hintText() {
            return whyNot.orElseGet(action::hint);
        }
    }
}
