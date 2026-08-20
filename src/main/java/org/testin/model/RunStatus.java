package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;
import org.testin.run.RunTestCases;

import java.util.Optional;
import java.util.List;
import javax.swing.*;
import java.awt.*;

@Getter
@AllArgsConstructor
public enum RunStatus {
    IDLE(
            AllIcons.RunConfigurations.TestState.Run,
            "Run Test Case",
            null
    ),

    PASSED(
            AllIcons.RunConfigurations.TestPassed,
            "Run Test Case",
            new Badge("Passed", new JBColor(new Color(100, 200, 100), new Color(50, 150, 50)))
    ),

    FAILED(
            AllIcons.RunConfigurations.TestFailed,
            "Run Test Case",
            new Badge("Failed", new JBColor(new Color(255, 100, 100), new Color(180, 50, 50)))
    ),

    RUNNING(
            AllIcons.Actions.Suspend,
            "Test case is Running...",
            new Badge("Running", new JBColor(new Color(255, 200, 100), new Color(200, 150, 50)))
    );

    private final @NotNull Icon icon;
    private final @NotNull String tooltip;

    /**
     * The card badge, null for IDLE - a case nobody has run carries no badge.
     * <p>
     * Label and color are one fact, so they are one field: a status cannot end
     * up with a label and no color to draw it in.
     */
    @Getter(AccessLevel.NONE)
    private final @Nullable Badge badge;

    /**
     * Runs the test case. No list is involved: this is the icon on a card or in
     * the details panel, and it already knows which case it is about.
     */
    public void executeAction(final @NotNull Project p, final @NotNull TestCaseDto dto) {
        RunTestCases.run(p, List.of(dto));
    }

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
