package org.testin.enums;

import com.intellij.ui.JBColor;
import lombok.Getter;
import org.testin.util.KeyboardSet;

import java.awt.*;
import java.util.Set;
import java.util.function.BiConsumer;

@Getter
public enum BugPriority {
    EMPTY(
            "",
            0,
            JBColor.background(),
            true,
            KeyboardSet.PriorityEmpty
    ),

    HIGH(
            "High",
            1,
            JBColor.RED.brighter().brighter(),
            true,
            KeyboardSet.PriorityHigh
    ),

    MEDIUM(
            "Medium",
            2,
            JBColor.BLUE.brighter(),
            true,
            KeyboardSet.PriorityMedium
    ),

    LOW(
            "Low",
            3,
            JBColor.GRAY.brighter(),
            true,
            KeyboardSet.PriorityLow
    );

    private final String name;
    private final int value;
    private final Color color;
    private final boolean active;
    private final KeyboardSet shortcut;
    private final BiConsumer<Set<BugPriority>, Boolean> action;

    BugPriority(final String name, final int value, final Color color, final boolean active, final KeyboardSet shortcut) {
        this.name = name;
        this.value = value;
        this.color = color;
        this.active = active;
        this.shortcut = shortcut;

        this.action = (set, state) -> {
            if (state) set.add(this);
            else set.remove(this);
        };

    }

    public void onChange(final Set<BugPriority> set, final boolean state) {
        action.accept(set, state);
    }
}