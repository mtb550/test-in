package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import java.awt.*;

@Getter
@AllArgsConstructor
public enum Priority {
    HIGH(
            "High",
            1,
            JBColor.RED.brighter().brighter(),
            true,
            Shortcuts.PriorityHigh
    ),

    MEDIUM(
            "Medium",
            2,
            JBColor.BLUE.brighter(),
            true,
            Shortcuts.PriorityMedium
    ),

    LOW(
            "Low",
            3,
            JBColor.GRAY.brighter(),
            true,
            Shortcuts.PriorityLow
    );

    private final @NotNull String name;
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
