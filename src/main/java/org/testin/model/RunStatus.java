package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import javax.swing.*;
import java.awt.*;

@Getter
@AllArgsConstructor
public enum RunStatus {
    IDLE(
            AllIcons.RunConfigurations.TestState.Run,
            null
    ),

    PASSED(
            AllIcons.RunConfigurations.TestPassed,
            new Badge("Passed", new JBColor(new Color(100, 200, 100), new Color(50, 150, 50)))
    ),

    FAILED(
            AllIcons.RunConfigurations.TestFailed,
            new Badge("Failed", new JBColor(new Color(255, 100, 100), new Color(180, 50, 50)))
    ),

    RUNNING(
            AllIcons.Actions.Suspend,
            new Badge("Running", new JBColor(new Color(255, 200, 100), new Color(200, 150, 50)))
    );

    /**
     * The status as the project tree draws it. What a card or the view panel
     * draws for the button beside it is that button's own icon, on
     * {@code CardHoverAction} - this one is the verdict, not the gesture.
     */
    private final @NotNull Icon icon;

    /**
     * The card badge, null for IDLE - a case nobody has run carries no badge.
     * <p>
     * Label and color are one fact, so they are one field: a status cannot end
     * up with a label and no color to draw it in.
     */
    @Getter(AccessLevel.NONE)
    private final @Nullable Badge badge;

    /**
     * The badge this status draws on a card, and empty for IDLE - a case nobody
     * has run carries none.
     */
    public @NotNull Optional<Badge> getBadge() {
        return Optional.ofNullable(badge);
    }

    public record Badge(@NotNull String label, @NotNull JBColor color) {
    }
}
