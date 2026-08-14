package org.testin.enums;

import com.intellij.ui.JBColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import java.awt.*;

@Getter
public enum BugPriority {
    EMPTY(
            "",
            0,
            JBColor.background(),
            true,
            Shortcuts.PriorityEmpty
    ),

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
    private final boolean active;
    private final @NotNull Shortcuts shortcut;

    // Same as Priority: active is always true here and the field is still a real
    // extension point, read through method references and genuinely false for
    // Group.UNASSIGNED. All three enums or none (#66, E3).
    @SuppressWarnings("SameParameterValue")
    BugPriority(final @NotNull String name, final int value, final @NotNull Color color, final boolean active, final @NotNull Shortcuts shortcut) {
        this.name = name;
        this.value = value;
        this.color = color;
        this.active = active;
        this.shortcut = shortcut;
    }
}
