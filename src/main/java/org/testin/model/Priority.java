package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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
            true
    ),

    MEDIUM(
            "P2",
            2,
            JBColor.BLUE.brighter(),
            true
    ),

    LOW(
            "P3",
            3,
            JBColor.GRAY.brighter(),
            true
    );

    private final @NotNull String label;
    private final int value;
    private final @NotNull Color color;
    // Always true here, and read through a method reference (Priority::isActive)
    // that a search for isActive() does not find - PrioritySection filters on it,
    // so deleting it reaches a failing compile rather than a warning.
    //
    // Group carried the same flag and no longer does: every group can be typed
    // now, so nothing read it (#200). This one is on its own until a priority is
    // retired, which is the case it exists for.
    private final boolean active;

}
