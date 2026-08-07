package org.testin.enums;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.Shared;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

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
            (item, project) -> item.getTc().getDescription(),
            null
    ),

    EXPECTED_RESULT(
            "Expected Result",
            true,
            true,
            (item, project) -> item.getTc().getExpectedResult(),
            null
    ),

    STEPS(
            "Steps",
            true,
            true,
            (item, project) -> String.join(", ", item.getTc().getSteps()),
            null
    ),

    PRIORITY(
            "Priority",
            true,
            true,
            (item, project) -> item.getTc().getPriority().getName(),
            item -> List.of(Shared.createPriorityBadge(item.getTc()))
    ),

    GROUP(
            "Group",
            true,
            true,
            (item, project) -> item.getTc().getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            item -> item.getTc().getGroup().stream().map(Shared::createGroupBadge).collect(Collectors.<JComponent>toList())
    ),

    ACTUAL_RESULT(
            "Actual Result",
            true,
            true,
            (item, project) -> item.getActualResult(),
            null
    ),

    BUG_SEVERITY(
            "Bug Severity",
            true,
            true,
            (item, project) -> item.getBugSeverity().getName(),
            null
    ),

    BUG_PRIORITY(
            "Bug Priority",
            true,
            true,
            (item, project) -> item.getBugPriority().getName(),
            null
    ),

    RUN_STATUS(
            "Run Status",
            true,
            true,
            (item, project) -> item.getStatus().getDisplayText(),
            null
    ),

    DURATION(
            "Duration",
            true,
            true,
            (item, project) -> {
                long s = item.getDuration().getSeconds();
                return String.format(Locale.ENGLISH, "%02d:%02d", (s % 3600) / 60, (s % 60));
            },
            null
    ),

    PATH(
            "Path",
            true,
            true,
            (item, project) -> {
                final TestCaseDto tc = Services.getInstance(project, ProjectIndexer.class).getTestCaseById(item.getId());
                return String.join(" > ", tc.getParent().getPath2());
            },
            null
    ),

    FQCN(
            "FQCN",
            true,
            true,
            (item, project) -> {
                final TestCaseDto tc = item.getTc();
                return String.join(" > ", Services.getInstance(project, Tools.class).buildFqcnMethod(tc));
            },
            null
    );

    private final String name;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final ValueExtractor valueExtractor;
    private final DrawItem drawItem;

    public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
        if (drawItem != null) badges.addAll(drawItem.apply(runItem));
        else details.put(name, valueExtractor.apply(runItem, p));
    }

    @FunctionalInterface
    public interface ValueExtractor {
        String apply(final TestRunItems item, final @NotNull Project p);
    }

    @FunctionalInterface
    public interface DrawItem {
        List<JComponent> apply(final TestRunItems item);
    }
}