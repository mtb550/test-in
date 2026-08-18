package org.testin.model;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.Shared;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum RunEditorAttributes {

    DESCRIPTION(
            "Description",
            true,
            (item, p) -> item.requireTc().getDescription()
    ),

    EXPECTED_RESULT(
            "Expected Result",
            true,
            (item, p) -> item.requireTc().getExpectedResult()
    ),

    STEPS(
            "Steps",
            false,
            (item, p) -> String.join(", ", item.requireTc().getSteps())
    ),

    PRIORITY(
            "Priority",
            true,
            (item, p) -> item.requireTc().getPriority().getName()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            badges.add(Shared.createPriorityBadge(runItem.requireTc()));
        }
    },

    GROUP(
            "Group",
            true,
            (item, p) -> item.requireTc().getGroup().stream().map(Group::getName).collect(Collectors.joining(", "))
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            runItem.requireTc().getGroup().stream().map(Shared::createGroupBadge).forEach(badges::add);
        }
    },

    ACTUAL_RESULT(
            "Actual Result",
            true,
            (item, p) -> item.getActualResult()
    ),

    BUG_SEVERITY(
            "Bug Severity",
            true,
            (item, p) -> item.getBugSeverity().getName()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            Shared.addBadge(badges, getName(), runItem.getBugSeverity().getName(), runItem.getBugSeverity().getColor());
        }
    },

    BUG_PRIORITY(
            "Bug Priority",
            true,
            (item, p) -> item.getBugPriority().getName()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            Shared.addBadge(badges, getName(), runItem.getBugPriority().getName(), runItem.getBugPriority().getColor());
        }
    },

    RUN_STATUS(
            "Run Status",
            true,
            (item, p) -> item.getStatus().getDisplayText()
    ),

    DURATION(
            "Duration",
            true,
            (item, p) -> Services.getInstance(p, Tools.class).getFormattedDuration(item.getDuration())
    ),

    EXECUTED_BY(
            "Executed By",
            false,
            (item, p) -> item.getExecutedBy()
    ),

    EXECUTED_AT(
            "Executed At",
            false,
            (item, p) -> Config.formatOrBlank(item.getExecutedAt())
    ),

    PATH(
            "Path",
            false,
            (item, p) -> {
                final TestCaseDto tc = Services.getInstance(p, ProjectIndexer.class).getTestCaseById(item.getId());
                if (tc != null)
                    return String.join(" > ", tc.getParent().getPath2());
                return "";
            }
    ),

    FQCN(
            "FQCN",
            false,
            (item, p) -> {
                final TestCaseDto tc = item.requireTc();
                return String.join(" > ", Services.getInstance(p, Tools.class).buildFqcnMethod(tc));
            }
    );

    private final @NotNull String name;
    private final boolean defaultToolBarSelected;
    private final @NotNull ValueExtractor<TestRunItems> runValueExtractor;

    /**
     * Renders as a plain detail row. The attributes drawn as badges override
     * this in their own body — the two behaviors sit on the constants that
     * have them instead of being chosen by a null at run time.
     */
    public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
        details.put(name, runValueExtractor.execute(runItem, p));
    }

}
