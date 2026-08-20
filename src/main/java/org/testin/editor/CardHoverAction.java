package org.testin.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.notifications.Notifier;
import org.testin.run.RunTestCases;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
@AllArgsConstructor
public enum CardHoverAction {
    NAVIGATE_TO_TEST_METHOD(
            "Navigate to Code",
            Shortcuts.NavigateToCode,
            OptionalPlugin.JAVA,
            AllIcons.Nodes.Class,
            NavigateToCodeAction::execute
    ),

    RUN_TEST_CASE(
            "Run Test Case",
            Shortcuts.RunTestCase,
            OptionalPlugin.TESTNG,
            AllIcons.RunConfigurations.TestState.Run,
            (p, tc) -> RunTestCases.run(p, List.of(tc))
    ),

    /**
     * Its own button rather than the run button drawing a different icon, and
     * never beside it: the card offers whichever of the two its state calls for.
     * <p>
     * Bound to no key: F5 runs a case and does not stop one, and
     * {@link Shortcuts#EMPTY} is what carries that without a reader having to
     * ask whether there is a shortcut at all.
     */
    STOP_TEST_CASE(
            "Stop Test Case",
            Shortcuts.EMPTY,
            OptionalPlugin.TESTNG,
            AllIcons.Actions.Suspend,
            (p, tc) -> stopRun(p)
    );

    private final @NotNull String tooltip;
    private final @NotNull Shortcuts shortcut;
    /**
     * The IDE plugin this action needs to do anything at all.
     */
    private final @NotNull OptionalPlugin requires;

    /**
     * What the button draws. On the action rather than at the painter: the
     * painter used to pick between a run icon and a stop icon itself, so the
     * icon on screen and the action behind it were two separate decisions and
     * could disagree - which is exactly how the stop icon came to do nothing
     * when clicked (#34).
     */
    private final @NotNull Icon icon;

    /**
     * What clicking the button does. Here rather than at each place that draws
     * one, so the card, the view panel and any later caller act the same.
     */
    @Getter(AccessLevel.NONE)
    private final @NotNull BiConsumer<Project, TestCaseDto> onClick;

    /**
     * Does this button's work on that test case.
     */
    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        onClick.accept(p, tc);
    }

    /**
     * Stops the run rather than the one case the button sits on: the plugin
     * holds no handle to a single case's process, and stopping one of a page of
     * twelve would leave the other eleven going with nothing to reach them by.
     */
    private static void stopRun(final @NotNull Project p) {
        Services.getInstance(p, TestNGExecution.class).stop();
        Services.getInstance(p, Notifier.class).softShow(p, "Stopped");
    }

    /**
     * Which of the two the card offers where a single run icon used to sit: the
     * stop while the case is running, the run every other time.
     * <p>
     * One owner, because three places have to name the same button - the painter
     * drawing it, the hit-test deciding what the pointer is over, and the click.
     */
    public static @NotNull CardHoverAction runSlot(final @NotNull RunStatus status) {
        return status == RunStatus.RUNNING ? STOP_TEST_CASE : RUN_TEST_CASE;
    }

    /**
     * What the card's tooltip says: the action, and the key that does it where
     * there is one. Built from the two rather than written out beside them, so a
     * shortcut that changes cannot leave the hint naming the old key - and the
     * button with no key needs no second spelling of its name.
     */
    public @NotNull String getHintText() {
        return (tooltip + " " + shortcut.getShortcutText()).trim();
    }

    /**
     * Whether this IDE offers the action. Asked before the icon is drawn and
     * before the pointer is asked what it is over, so in PyCharm or GoLand the
     * icon is absent rather than present and answering with a balloon (#66).
     */
    public boolean isOffered() {
        return requires.isAvailable();
    }
}
