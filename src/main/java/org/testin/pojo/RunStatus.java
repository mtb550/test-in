package org.testin.pojo;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.actions.RunTestCase;
import org.testin.pojo.dto.TestCaseDto;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.BiConsumer;

@Getter
@AllArgsConstructor
public enum RunStatus {
    IDLE(
            "IDLE",
            AllIcons.RunConfigurations.TestState.Run,
            "Run Test Case",
            (tc, list) -> {}
    ),

    PASSED(
            "PASSED",
            AllIcons.RunConfigurations.TestPassed,
            "Run Test Case",
            (tc, list) -> {}
    ),

    FAILED("FAILED",
            AllIcons.RunConfigurations.TestFailed,
            "Run Test Case",
            (tc, list) -> {}
    ),

    RUNNING(
            "RUNNING",
            AllIcons.Actions.Suspend,
            "Test case is Running...",
            (tc, list) -> {
            }
    );

    private final String statusName;
    private final Icon icon;
    private final String tooltip;
    private final BiConsumer<TestCaseDto, JBList<TestCaseDto>> action;

    public static RunStatus fromString(final String status) {
        if (status == null || status.trim().isEmpty()) {
            return IDLE;
        }
        for (RunStatus rs : values()) {
            if (rs.statusName.equalsIgnoreCase(status)) {
                return rs;
            }
        }
        return IDLE;
    }

    public void executeAction(final @NotNull Project project, final TestCaseDto dto, final JBList<TestCaseDto> list) {
        new RunTestCase(list).execute(project, dto);
    }
}