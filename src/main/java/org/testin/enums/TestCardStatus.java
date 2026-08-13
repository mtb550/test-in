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

    public static @Nullable TestCardStatus from(final String status) {
        for (final TestCardStatus s : values())
            if (s.name().equals(status)) return s;

        return null;
    }
}
