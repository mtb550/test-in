package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import java.awt.*;

/**
 * How much a test case matters, as P1 to P3.
 * <p>
 * The word was the label until now - High, Medium, Low - and it read as the same
 * scale as the bug's priority sitting beside it on a run row. P1 is the shape a
 * tester already knows from every tracker, it is shorter on a badge that no
 * longer carries a caption, and it cannot be confused with the bug's High.
 * <p>
 * The constants keep their names. They are what Jackson writes into every test
 * case file, so renaming them would rewrite the meaning of data already on disk
 * to buy nothing - the label is what anybody reads.
 */
@Getter
@AllArgsConstructor
public enum Priority {
    HIGH(
            "P1",
            1,
            JBColor.RED.brighter().brighter(),
            true,
            Shortcuts.PriorityHigh
    ),

    MEDIUM(
            "P2",
            2,
            JBColor.BLUE.brighter(),
            true,
            Shortcuts.PriorityMedium
    ),

    LOW(
            "P3",
            3,
            JBColor.GRAY.brighter(),
            true,
            Shortcuts.PriorityLow
    );

    private final @NotNull String label;
    private final int value;
    private final @NotNull Color color;
    // Always true here, and still a real extension point rather than dead: it is
    // read through method references (Priority::isActive, Group::isActive) that a
    // search for isActive() does not find, and Group.UNASSIGNED in the third enum
    // of the set is genuinely inactive. Deleting it once already reached a
    // failing compile - remove it from all three enums together or not at all
    // (#66, E3).
    private final boolean active;
    private final @NotNull Shortcuts shortcut;

}
