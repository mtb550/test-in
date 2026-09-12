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

import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.notifications.Notifier;
import org.testin.runner.RunTestCases;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.actions.Declared;
import org.testin.services.OptionalPlugin;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
@AllArgsConstructor
public enum CardHoverAction {
    NAVIGATE_TO_TEST_METHOD(
            Bundle.message("automated.navigate"),
            "Testin.NavigateToCode",
            List.of(OptionalPlugin.JAVA),
            AllIcons.Nodes.Class,
            (p, cases) -> NavigateToCodeAction.execute(p, cases.getFirst())
    ),

    RUN_TEST_CASE(
            Bundle.message("card.run.test.case"),
            // Both. TestNG starts the run and Java finds the method it starts, so
            // an IDE with only TestNG offered Run and then resolved every case to
            // nothing - one "has no generated code yet" per case, with no
            // mention of the plugin that was missing (#248).
            "Testin.RunTestCase",
            List.of(OptionalPlugin.JAVA, OptionalPlugin.TESTNG),
            AllIcons.RunConfigurations.TestState.Run,
            RunTestCases::run
    ),

    /**
     * Its own button rather than the run button drawing a different icon, and
     * never beside it: the card offers whichever of the two its state calls for.
     * <p>
     * Bound to no key: F5 runs a case and does not stop one, and there is no
     * declared action behind it either - an empty id is what carries that
     * without a reader having to ask whether there is a shortcut at all.
     */
    STOP_TEST_CASE(
            Bundle.message("card.stop.test.case"),
            "",
            List.of(OptionalPlugin.TESTNG),
            AllIcons.Actions.Suspend,
            CardHoverAction::stopRun
    );

    private final @NotNull String tooltip;
    /**
     * The declared action this gesture is, by the id {@code plugin.xml} gives
     * it, and empty for a gesture no action stands behind.
     * <p>
     * An id rather than a keystroke, so the hint prints whatever the tester has
     * bound rather than what we shipped (#119).
     */
    private final @NotNull String actionId;
    /**
     * The IDE plugins this action needs to do anything at all - every one of
     * them, not the most obvious one.
     */
    @Getter(AccessLevel.NONE)
    private final @NotNull List<OptionalPlugin> requires;

    /**
     * What the button draws. On the action rather than at the painter: the
     * painter used to pick between a run icon and a stop icon itself, so the
     * icon on screen and the action behind it were two separate decisions and
     * could disagree - which is exactly how the stop icon came to do nothing
     * when clicked (#34).
     */
    private final @NotNull Icon icon;

    /**
     * What pressing the button does. Here rather than at each place that offers
     * one, so the card, the view panel, the context menu and the key all act the
     * same.
     * <p>
     * A selection rather than a case, because that is the widest a caller has:
     * the keyboard hands over what is selected, and a card hands over the one it
     * sits on. Running twelve cases as one call is also what keeps the plugin to
     * a single notification with a count.
     */
    @Getter(AccessLevel.NONE)
    private final @NotNull BiConsumer<Project, List<TestCaseDto>> onClick;

    /**
     * UC-CODEGEN-009, Rule-CODEGEN-005.
     * <p>
     * Leaves a menu entry alone when every plugin this action needs is there,
     * and grays it with the first missing one named when they are not.
     * <p>
     * Asked here rather than at each menu, because what an action needs is the
     * action's own knowledge - the two context menus were deciding it for
     * themselves, and one of them had it wrong. The card already asked the same
     * list through {@link #isOffered}; the menus did not, which is how Run came
     * to be offered in an IDE that could not resolve a single case (#248).
     */
    public boolean enableOrExplain(final @NotNull Presentation presentation) {
        return requires.stream().allMatch(plugin -> plugin.enableOrExplain(presentation, tooltip));
    }

    /**
     * Does this button's work on the one case it was pressed from.
     */
    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        execute(p, List.of(tc));
    }

    /**
     * Does this button's work on a whole selection.
     */
    public void execute(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        if (cases.isEmpty()) return;

        onClick.accept(p, cases);
    }

    /**
     * UC-EDITOR-PANEL-043.
     * <p>
     * Stops these cases and no others: a card stops the one it sits on, and the
     * key stops what is selected. Said once with a count, the way starting them
     * is - a page of twelve raises one notification, not twelve.
     */
    private static void stopRun(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final int stopped = Services.getInstance(p, TestNGExecution.class).stop(cases);

        if (stopped > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, Done.STOPPED, stopped);
    }

    /**
     * Which of the two a case offers where a single run icon used to sit: the
     * stop while it is running, the run every other time.
     */
    public static @NotNull CardHoverAction runSlot(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return runSlot(p, List.of(tc));
    }

    /**
     * UC-EDITOR-PANEL-043.
     * <p>
     * Which of the two a whole selection offers: the stop as soon as one of them
     * is running, because a stop stops the run and not the case it was asked
     * from.
     * <p>
     * One owner, because everything that offers this gesture has to name the
     * same button - the painter drawing it, the hit-test deciding what the
     * pointer is over, the click, the context menu entry, and F5. The card and
     * the key disagreed for exactly as long as they answered this separately
     * (#66, finding 18).
     * <p>
     * Asked of the runner, not of the DTO: the DTO's temp status dies with its
     * instance on every rescan, and a card that then offered Run on a running
     * case would start it twice (#116).
     */
    public static @NotNull CardHoverAction runSlot(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        return cases.stream().anyMatch(tc -> execution.isRunning(tc.getId()))
                ? STOP_TEST_CASE
                : RUN_TEST_CASE;
    }

    /**
     * What the card's tooltip says: the action, and the key that does it where
     * there is one. Built from the two rather than written out beside them, so a
     * shortcut that changes cannot leave the hint naming the old key - and the
     * button with no key needs no second spelling of its name.
     * <p>
     * The key is asked of the keymap, so this is the tester's own binding rather
     * than the default we shipped (#119).
     */
    public @NotNull String getHintText() {
        return (tooltip + " " + Declared.shortcutText(actionId)).trim();
    }

    /**
     * Whether this IDE offers the action. Asked before the icon is drawn and
     * before the pointer is asked what it is over, so in PyCharm or GoLand the
     * icon is absent rather than present and answering with a balloon (#66).
     */
    public boolean isOffered() {
        return requires.stream().allMatch(OptionalPlugin::isAvailable);
    }
}
