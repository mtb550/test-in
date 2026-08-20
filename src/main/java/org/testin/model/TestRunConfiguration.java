package org.testin.model;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum TestRunConfiguration {

    TEST_TYPE(
            "Test Type",
            AllIcons.Nodes.Type,
            new String[]{"", "Functional Test", "Performance Test"}
    ),

    CHANGE_LOG(
            "Change Log",
            AllIcons.Nodes.Type,
            Free.OPTIONS
    ),

    COMMIT_ID(
            "Commit ID",
            AllIcons.Nodes.Type,
            Free.OPTIONS
    ),

    PLATFORM(
            "Platform",
            AllIcons.Nodes.PpLib,
            new String[]{"", "Web", "Mobile"}
    ),

    COMPONENT(
            "Component",
            AllIcons.Nodes.PpLib,
            new String[]{"", "Frontend", "Backend"}
    ),

    LANGUAGE(
            "Language",
            AllIcons.Nodes.Lambda,
            new String[]{"", "English", "Arabic", "French"}
    ),

    BROWSER(
            "Browser",
            AllIcons.Nodes.WebFolder,
            new String[]{"", "Chrome", "Firefox", "Safari", "Edge"}
    ),

    DEVICE_TYPE(
            "Device Type",
            AllIcons.Nodes.Include,
            new String[]{"", "Desktop", "Mobile", "Tablet"}
    );

    private final @NotNull String displayName;
    private final @NotNull Icon icon;

    /**
     * Null for free-text fields; set only by those offering a fixed choice.
     */
    /**
     * A field with nothing to pick from: a line to type in.
     * <p>
     * In a holder because an enum constant cannot name a static field of its own
     * enum, and the constants are declared first.
     */
    private static final class Free {
        private static final String[] OPTIONS = new String[0];
    }

    /**
     * What the field offers to pick from, and nothing at all for a field that is
     * free text - which is what {@link #isChoice()} answers.
     */
    private final @NotNull String[] options;

    /**
     * True when this field is a dropdown rather than a line to type in.
     */
    public boolean isChoice() {
        return options.length > 0;
    }
}
