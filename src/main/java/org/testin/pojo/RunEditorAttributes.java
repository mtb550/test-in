package org.testin.pojo;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.editorPanel.Shared;
import org.testin.util.Tools;
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

    RUN_STATUS(
            "Run Status",
            true,
            true,
            (item, project) -> item.getStatus().name(),
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
            (item, project) -> String.join(" > ", item.getPath()),
            null
    ),

    FQCN(
            "FQCN",
            true,
            true,
            (item, project) -> String.join(" > ", Services.getInstance(project, Tools.class).buildFqcn(item.getTc())), //todo, to be updated later
            null
    );

    private final String name;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final ValueExtractor valueExtractor;
    private final DrawItem drawItem;

    public void applyToUI(final TestRunItems runItem, final List<JComponent> badges, final Map<String, String> details, final Project project) {
        if (drawItem != null) badges.addAll(drawItem.apply(runItem));
        else details.put(name, valueExtractor.apply(runItem, project));
    }

    @FunctionalInterface
    public interface ValueExtractor {
        String apply(final TestRunItems item, final Project project);
    }

    @FunctionalInterface
    public interface DrawItem {
        List<JComponent> apply(final TestRunItems item);
    }
}