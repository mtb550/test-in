package testGit.pojo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import testGit.actions.RunTestCase;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public enum RunStatus {
    IDLE(
            "IDLE",
            AllIcons.RunConfigurations.TestState.Run,
            "Run Test Case",
            RunTestCase::execute
    ),

    PASSED(
            "PASSED",
            AllIcons.RunConfigurations.TestPassed,
            "Run Test Case",
            RunTestCase::execute
    ),

    FAILED("FAILED",
            AllIcons.RunConfigurations.TestFailed,
            "Run Test Case",
            RunTestCase::execute
    ),

    RUNNING(
            "RUNNING",
            AllIcons.Actions.Suspend,
            "Test case is Running...",
            dto -> {
            }
    );

    private final String statusName;
    private final Icon icon;
    private final String tooltip;
    private final Consumer<TestCaseDto> action;

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

    public void executeAction(final TestCaseDto dto) {
        if (action != null) {
            action.accept(dto);
        }
    }
}