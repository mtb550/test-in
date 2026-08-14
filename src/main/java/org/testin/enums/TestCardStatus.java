package org.testin.enums;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

@Getter
@AllArgsConstructor
public enum TestCardStatus {
    RUNNING(
            "Running",
            new JBColor(new Color(255, 200, 100), new Color(200, 150, 50))
    ),

    PASSED(
            "Passed",
            new JBColor(new Color(100, 200, 100), new Color(50, 150, 50))
    ),

    FAILED(
            "Failed",
            new JBColor(new Color(255, 100, 100), new Color(180, 50, 50))
    );

    private final @NotNull String label;
    private final @NotNull JBColor badgeColor;

    /**
     * The badge for a run status, or null for the ones that get none.
     * <p>
     * Matched by name: this enum names the same three states RunStatus does and
     * adds a label and a colour to them. Worth merging into it.
     */
    public static @Nullable TestCardStatus from(final @NotNull RunStatus status) {
        for (final TestCardStatus s : values())
            if (s.name().equals(status.name())) return s;

        return null;
    }
}
