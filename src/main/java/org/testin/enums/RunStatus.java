package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.run.RunTestCaseAction;

import javax.swing.*;
import java.util.function.BiConsumer;

@Getter
@AllArgsConstructor
public enum RunStatus {
    IDLE(
            "IDLE",
            AllIcons.RunConfigurations.TestState.Run,
            "Run Test Case",
            (tc, list) -> {
            }
    ),

    PASSED(
            "PASSED",
            AllIcons.RunConfigurations.TestPassed,
            "Run Test Case",
            (tc, list) -> {
            }
    ),

    FAILED("FAILED",
            AllIcons.RunConfigurations.TestFailed,
            "Run Test Case",
            (tc, list) -> {
            }
    ),

    RUNNING(
            "RUNNING",
            AllIcons.Actions.Suspend,
            "Test case is Running...",
            (tc, list) -> {
            }
    );

    private final @NotNull String statusName;
    private final @NotNull Icon icon;
    private final @NotNull String tooltip;
    private final @NotNull BiConsumer<TestCaseDto, JBList<TestCaseDto>> action;

    public static @NotNull RunStatus fromString(final @Nullable String status) {
        if (status == null || status.trim().isEmpty()) {
            return IDLE;
        }
        for (final RunStatus rs : values()) {
            if (rs.statusName.equalsIgnoreCase(status)) {
                return rs;
            }
        }
        return IDLE;
    }

    public void executeAction(final @NotNull Project p, final @NotNull TestCaseDto dto,
                              final @NotNull JBList<TestCaseDto> list) {
        new RunTestCaseAction(p, list).execute(dto);
    }
}
