package org.testin.enums;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.Shared;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import javax.swing.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum RunEditorAttributes {

    DESCRIPTION(
            "Description",
            true,
            true,
            (item, p) -> item.getTc().getDescription(),
            null
    ),

    EXPECTED_RESULT(
            "Expected Result",
            true,
            true,
            (item, p) -> item.getTc().getExpectedResult(),
            null
    ),

    STEPS(
            "Steps",
            true,
            true,
            (item, p) -> String.join(", ", item.getTc().getSteps()),
            null
    ),

    PRIORITY(
            "Priority",
            true,
            true,
            (item, p) -> item.getTc().getPriority().getName(),
            item -> List.of(Shared.createPriorityBadge(item.getTc()))
    ),

    GROUP(
            "Group",
            true,
            true,
            (item, p) -> item.getTc().getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            item -> item.getTc().getGroup().stream().map(Shared::createGroupBadge).collect(Collectors.<JComponent>toList())
    ),

    ACTUAL_RESULT(
            "Actual Result",
            true,
            true,
            (item, p) -> item.getActualResult(),
            null
    ),

    BUG_SEVERITY(
            "Bug Severity",
            true,
            true,
            (item, p) -> item.getBugSeverity().getName(),
            null
    ),

    BUG_PRIORITY(
            "Bug Priority",
            true,
            true,
            (item, p) -> item.getBugPriority().getName(),
            null
    ),

    RUN_STATUS(
            "Run Status",
            true,
            true,
            (item, p) -> item.getStatus().getDisplayText(),
            null
    ),

    DURATION(
            "Duration",
            true,
            true,
            (item, p) -> {
                long s = item.getDuration().getSeconds();
                return String.format(Locale.ENGLISH, "%02d:%02d", (s % 3600) / 60, (s % 60));
            },
            null
    ),

    PATH(
            "Path",
            true,
            true,
            (item, p) -> {
                final TestCaseDto tc = Services.getInstance(p, ProjectIndexer.class).getTestCaseById(item.getId());
                if (tc != null)
                    return String.join(" > ", tc.getParent().getPath2());
                return "";
            },
            null
    ),

    FQCN(
            "FQCN",
            true,
            true,
            (item, p) -> {
                final TestCaseDto tc = item.getTc();
                return String.join(" > ", Services.getInstance(p, Tools.class).buildFqcnMethod(tc));
            },
            null
    );

    private final @NotNull String name;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final @NotNull ValueExtractor<TestRunItems> runValueExtractor;
    private final @NotNull DrawItem<TestRunItems> runDrawItem;

    public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
        if (runDrawItem != null) badges.addAll(runDrawItem.execute(runItem));
        else details.put(name, runValueExtractor.execute(runItem, p));
    }

}
