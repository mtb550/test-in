package testGit.pojo;

import com.intellij.ui.JBColor;
import lombok.Getter;
import testGit.util.KeyboardSet;

import java.awt.*;
import java.util.Set;
import java.util.function.BiConsumer;

@Getter
public enum Priority {
    HIGH(
            "High",
            JBColor.RED.brighter().brighter(),
            true,
            KeyboardSet.PriorityHigh
    ),

    MEDIUM(
            "Medium",
            JBColor.BLUE.brighter(),
            true,
            KeyboardSet.PriorityMedium
    ),

    LOW(
            "Low",
            JBColor.GRAY.brighter(),
            true,
            KeyboardSet.PriorityLow
    );

    private final String name;
    private final Color color;
    private final boolean active;
    private final KeyboardSet shortcut;
    private final BiConsumer<Set<Priority>, Boolean> action;

    Priority(final String name, final Color color, final boolean active, final KeyboardSet shortcut) {
        this.name = name;
        this.color = color;
        this.active = active;
        this.shortcut = shortcut;

        this.action = (set, state) -> {
            if (state) set.add(this);
            else set.remove(this);
        };

    }

    public void onChange(final Set<Priority> set, final boolean state) {
        action.accept(set, state);
    }
}