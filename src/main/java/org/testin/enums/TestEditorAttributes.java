package org.testin.enums;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.Shared;
import org.testin.importExport.imports.ImportSetter;
import org.testin.mappers.Config;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: add order, then add it toolbar details (select by the order number) & add it to edit menu.
// TODO: add all to edit menu: auto ref, business ref..etc
// TODO: also, map all to view panel dynamically.
// TODO: may you need to unify enum map to map all with one source of truth
@Getter
@AllArgsConstructor
public enum TestEditorAttributes {

    ID(
            "ID",
            "ID:",
            true,
            false,
            false,
            false,
            true,
            (tc, project) -> String.valueOf(tc.getId()),
            null,
            (project, tc, v) -> {
            }
    ),

    /// TODO:: added to tool bar details, to be shown but disabled
    DESCRIPTION(
            "Description",
            "Description:",
            true,
            true,
            true,
            true,
            true,
            (tc, project) -> tc.getDescription(),
            null,
            (project, tc, v) -> tc.setDescription(Services.getInstance(project, Tools.class).sanitizeDescription(v))
    ),

    EXPECTED_RESULT(
            "Expected Result",
            "Expected Result:",
            true,
            true,
            true,
            false,
            true,
            (tc, project) -> tc.getExpectedResult(),
            null,
            (project, tc, v) -> tc.setExpectedResult(v)
    ),

    STEPS(
            "Steps",
            "Steps:",
            true,
            true,
            true,
            false,
            true,
            (tc, project) -> String.join(", ", tc.getSteps()),
            null,
            (project, tc, v) -> tc.setSteps(Services.getInstance(project, Tools.class).parseStepsSafe(v))
    ),

    PRIORITY(
            "Priority",
            "Priority:",
            true,
            true,
            true,
            false,
            true,
            (tc, project) -> tc.getPriority().getName(),
            tc -> List.of(Shared.createPriorityBadge(tc)),
            (project, tc, v) -> tc.setPriority(Services.getInstance(project, Tools.class).parsePrioritySafe(v))
    ),

    FQCN(
            "FQCN",
            "FQCN:",
            true,
            true,
            false,
            false,
            true,
            (tc, project) -> String.join(" > ", Services.getInstance(project, Tools.class).buildFqcnMethod(tc)),
            null,
            (project, tc, v) -> {
            }
    ),

    REFERENCE(
            "Reference",
            "Reference:",
            true,
            false,
            true,
            false,
            true,
            (tc, project) -> tc.getReference(),
            null,
            (project, tc, v) -> tc.setReference(v)
    ),

    TEST_DATA(
            "Test Data",
            "Test Data:",
            true,
            false,
            true,
            false,
            true,
            (tc, project) -> tc.getTestData(),
            null,
            (project, tc, v) -> tc.setTestData(v)
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            "Pre Conditions:",
            true,
            false,
            true,
            false,
            true,
            (tc, project) -> tc.getPreConditions(),
            null,
            (project, tc, v) -> tc.setPreConditions(v)
    ),

    GROUP(
            "Group",
            "Group:",
            true,
            true,
            true,
            false,
            true,
            (tc, project) -> tc.getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            tc -> tc.getGroup().stream().map(Shared::createGroupBadge).collect(Collectors.<JComponent>toList()),
            (project, tc, v) -> tc.setGroup(Services.getInstance(project, Tools.class).parseGroupsSafe(v))
    ),

    PATH(
            "Path",
            "Path:",
            true,
            false,
            false,
            false,
            true,
            (tc, project) -> String.join(" > ", tc.getParent().getPath2()),
            null,
            (project, tc, v) -> {
            }
    ),

    ///  TODO:: ORDER to be added to show or hide sequence numbers in editors

    MODULE(
            "Module",
            "Module:",
            true,
            false,
            true,
            false,
            true,
            (tc, project) -> tc.getModule(),
            null,
            (project, tc, v) -> tc.setModule(v)
    ),

    STATUS(
            "Status",
            "Status:",
            true,
            false,
            false,
            false,
            true,
            (tc, project) -> tc.getStatus().getDisplayText(),
            null,
            (project, tc, v) -> tc.setStatus(TestCaseStatus.valueOf(v))
    ),

    CREATE_BY(
            "Created By",
            "Created By:",
            true,
            false,
            true,
            false,
            true,
            (tc, project) -> tc.getCreatedBy(),
            null,
            (project, tc, v) -> tc.setCreatedBy(v)
    ),

    UPDATE_BY(
            "Updated By",
            "Updated By:",
            true,
            false,
            true,
            false,
            true,
            (tc, project) -> tc.getUpdatedBy(),
            null,
            (project, tc, v) -> tc.setUpdatedBy(v)
    ),

    CREATE_AT(
            "Created At",
            "Created At:",
            true,
            false,
            true,
            false,
            true,
            (tc, project) -> tc.getCreatedAt().format(Config.getDateFormatterPattern()),
            null,
            (project, tc, v) -> tc.setCreatedAt(Services.getInstance(project, Tools.class).parseDateSafe(v))
    ),

    UPDATE_AT(
            "Updated At",
            "Updated At:",
            true,
            false,
            true,
            false,
            true,
            (tc, project) -> tc.getUpdatedAt().format(Config.getDateFormatterPattern()),
            null,
            (project, tc, v) -> tc.setUpdatedAt(Services.getInstance(project, Tools.class).parseDateSafe(v))
    );

    private final String name;
    private final String name2;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final boolean importable;
    private final boolean copyable;
    private final boolean exportable;
    private final ValueExtractor valueExtractor;
    private final DrawItem drawItem;
    private final ImportSetter importSetter;

    public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project project) {
        if (drawItem != null) badges.addAll(drawItem.apply(tc));
        else details.put(name, valueExtractor.apply(tc, project));
    }

    @FunctionalInterface
    public interface ValueExtractor {
        String apply(final @NotNull TestCaseDto tc, final @NotNull Project project);
    }

    @FunctionalInterface
    public interface DrawItem {
        List<JComponent> apply(final @NotNull TestCaseDto tc);
    }
}