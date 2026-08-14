package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.run.RunTestCaseAction;

import javax.swing.*;
import java.awt.*;

@Getter
@AllArgsConstructor
public enum RunStatus {
    IDLE(
            AllIcons.RunConfigurations.TestState.Run,
            "Run Test Case",
            "",
            null
    ),

    PASSED(
            AllIcons.RunConfigurations.TestPassed,
            "Run Test Case",
            "Passed",
            new JBColor(new Color(100, 200, 100), new Color(50, 150, 50))
    ),

    FAILED(
            AllIcons.RunConfigurations.TestFailed,
            "Run Test Case",
            "Failed",
            new JBColor(new Color(255, 100, 100), new Color(180, 50, 50))
    ),

    RUNNING(
            AllIcons.Actions.Suspend,
            "Test case is Running...",
            "Running",
            new JBColor(new Color(255, 200, 100), new Color(200, 150, 50))
    );

    private final @NotNull Icon icon;
    private final @NotNull String tooltip;

    /**
     * The card badge for this status, empty for IDLE - a case nobody has run
     * carries no badge at all.
     */
    private final @NotNull String badgeLabel;

    /**
     * Null only for IDLE, which draws no badge to colour.
     */
    private final @Nullable JBColor badgeColor;

    public boolean hasBadge() {
        return badgeColor != null;
    }


    /**
     * Runs the test case. The list is the component the run action binds its
     * shortcut to and is null when there is none — the details panel runs a
     * single case it already holds, with no list behind it.
     */
    public void executeAction(final @NotNull Project p, final @NotNull TestCaseDto dto,
                              final @Nullable JBList<TestCaseDto> list) {
        new RunTestCaseAction(p, list).execute(dto);
    }
}
