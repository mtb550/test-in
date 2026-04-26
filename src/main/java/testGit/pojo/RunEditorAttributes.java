package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import testGit.editorPanel.Shared;
import testGit.pojo.dto.TestRunDto;

import javax.swing.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum RunEditorAttributes {

    DESCRIPTION(
            "Description",
            true,
            true,
            item -> item.getTestCaseDetails().getDescription(),
            null
    ),

    EXPECTED_RESULT(
            "Expected Result",
            true,
            true,
            item -> item.getTestCaseDetails().getExpectedResult(),
            null
    ),

    STEPS(
            "Steps",
            true,
            true,
            item -> String.join(", ", item.getTestCaseDetails().getSteps()),
            null
    ),

    PRIORITY(
            "Priority",
            true,
            true,
            item -> item.getTestCaseDetails().getPriority().getName(),
            item -> List.of(Shared.createPriorityBadge(item.getTestCaseDetails()))
    ),

    GROUP(
            "Group",
            true,
            true,
            item -> item.getTestCaseDetails().getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            item -> item.getTestCaseDetails().getGroup().stream().map(Shared::createGroupBadge).collect(Collectors.<JComponent>toList())
    ),

    ACTUAL_RESULT(
            "Actual Result",
            true,
            true,
            TestRunDto.TestRunItems::getActualResult,
            null
    ),

    RUN_STATUS(
            "Run Status",
            true,
            true,
            item -> item.getStatus().name(),
            null
    ),

    DURATION(
            "Duration",
            true,
            true,
            item -> {
                long s = item.getDuration().getSeconds();
                return String.format(Locale.ENGLISH, "%02d:%02d", (s % 3600) / 60, (s % 60));
            },
            null
    ),

    PATH(
            "Path",
            true,
            true,
            item -> String.join(".", item.getTestCaseDetails().getPath()),
            null
    );

    private final String name;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final Function<TestRunDto.TestRunItems, String> valueExtractor;
    private final Function<TestRunDto.TestRunItems, List<JComponent>> drawItem;

    public void applyToUI(final TestRunDto.TestRunItems runItem, final List<JComponent> badges, final Map<String, String> details) {
        if (drawItem != null) badges.addAll(drawItem.apply(runItem));
        else details.put(name, valueExtractor.apply(runItem));
    }
}