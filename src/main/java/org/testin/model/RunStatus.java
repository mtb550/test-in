package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum RunStatus {
    IDLE(
            AllIcons.RunConfigurations.TestState.Run,
            Badge.NONE,
            Optional.empty()
    ),

    PASSED(
            AllIcons.RunConfigurations.TestPassed,
            new Badge(TestStatus.PASSED.getLabel(), new JBColor(new Color(100, 200, 100), new Color(50, 150, 50))),
            Optional.of(TestStatus.PASSED)
    ),

    FAILED(
            AllIcons.RunConfigurations.TestFailed,
            new Badge(TestStatus.FAILED.getLabel(), new JBColor(new Color(255, 100, 100), new Color(180, 50, 50))),
            Optional.of(TestStatus.FAILED)
    ),

    RUNNING(
            AllIcons.Actions.Suspend,
            new Badge("Running", new JBColor(new Color(255, 200, 100), new Color(200, 150, 50))),
            Optional.empty()
    );

    /**
     * The status as the project tree draws it. What a card or the view panel
     * draws for the button beside it is that button's own icon, on
     * {@code CardHoverAction} - this one is the verdict, not the gesture.
     */
    private final @NotNull Icon icon;

    /**
     * The card badge, and {@link Badge#NONE} for IDLE - a case nobody has run
     * carries none.
     * <p>
     * Label and color are one fact, so they are one field: a status cannot end
     * up with a label and no color to draw it in.
     */
    private final @NotNull Badge badge;

    /**
     * The verdict this report writes into a test run, and empty for a report
     * that is not one.
     * <p>
     * A case that has just started has no verdict yet, and one a stop put back
     * did not fail - nobody found a defect, it simply did not finish (#34). So
     * two of the four carry nothing, said with an empty value rather than a null
     * the run editor would have to test for.
     * <p>
     * Declared here so the run editor never maps one vocabulary to the other by
     * hand: what a TestNG report means for a run is this enum's to say.
     */
    private final @NotNull Optional<TestStatus> verdict;

    /**
     * Whether this status draws a badge at all. The one reader of what the badge
     * is, so no caller has to know that "nobody has run this" and "there is
     * nothing to draw" are the same fact.
     */
    public boolean hasBadge() {
        return badge != Badge.NONE;
    }

    public record Badge(@NotNull String label, @NotNull JBColor color) {

        /**
         * The badge of a case nobody has run: no word and no color to draw it
         * in. Never drawn - {@link #hasBadge()} is what decides that - and here
         * so IDLE says it carries none with a value of its own type.
         */
        public static final @NotNull Badge NONE = new Badge("", JBColor.GRAY);
    }
}
