package org.testin.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testin.editorPanel.Shared;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
            tc -> String.valueOf(tc.getId()),
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
            TestCaseDto::getDescription,
            null,
            (project, tc, v) -> tc.setDescription(Services.getInstance(project, Tools.class).sanitizeDescription(v))
    ),

    EXPECTED_RESULT(
            "Expected Result",
            "Expected Result:",
            true,
            true,
            true,
            TestCaseDto::getExpectedResult,
            null,
            (project, tc, v) -> tc.setExpectedResult(v)
    ),

    STEPS(
            "Steps",
            "Steps:",
            true,
            true,
            true,
            tc -> String.join(", ", tc.getSteps()),
            null,
            (project, tc, v) -> tc.setSteps(Services.getInstance(project, Tools.class).parseStepsSafe(v))
    ),

    PRIORITY(
            "Priority",
            "Priority:",
            true,
            true,
            true,
            tc -> tc.getPriority().getName(),
            tc -> List.of(Shared.createPriorityBadge(tc)),
            (project, tc, v) -> tc.setPriority(Services.getInstance(project, Tools.class).parsePrioritySafe(v))
    ),

    FQCN(
            "FQCN",
            "FQCN:",
            true,
            true,
            false,
            tc -> String.join(" > ", tc.getFqcn()),
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
            TestCaseDto::getReference,
            null,
            (project, tc, v) -> tc.setReference(v)
    ),

    TEST_DATA(
            "Test Data",
            "Test Data:",
            true,
            false,
            true,
            TestCaseDto::getTestData,
            null,
            (project, tc, v) -> tc.setTestData(v)
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            "Pre Conditions:",
            true,
            false,
            true,
            TestCaseDto::getPreConditions,
            null,
            (project, tc, v) -> tc.setPreConditions(v)
    ),

    GROUP(
            "Group",
            "Group:",
            true,
            true,
            true,
            tc -> tc.getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            tc -> tc.getGroup().stream().map(Shared::createGroupBadge).collect(Collectors.<JComponent>toList()),
            (project, tc, v) -> tc.setGroup(Services.getInstance(project, Tools.class).parseGroupsSafe(v))
    ),

    PATH(
            "Path",
            "Path:",
            true,
            false,
            false,
            tc -> String.join(" > ", tc.getPath()),
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
            TestCaseDto::getModule,
            null,
            (project, tc, v) -> tc.setModule(v)
    ),

    STATUS(
            "Status",
            "Status:",
            true,
            false,
            true,
            TestCaseDto::getTempStatus,
            null,
            (project, tc, v) -> tc.setStatus(v)
    ),

    CREATE_BY(
            "Created By",
            "Created By:",
            true,
            false,
            true,
            TestCaseDto::getCreatedBy,
            null,
            (project, tc, v) -> tc.setCreatedBy(v)
    ),

    UPDATE_BY(
            "Updated By",
            "Updated By:",
            true,
            false,
            true,
            TestCaseDto::getUpdatedBy,
            null,
            (project, tc, v) -> tc.setUpdatedBy(v)
    ),

    CREATE_AT(
            "Created At",
            "Created At:",
            true,
            false,
            true,
            tc -> tc.getCreatedAt().format(Config.getDateFormatterPattern()),
            null,
            (project, tc, v) -> tc.setCreatedAt(Services.getInstance(project, Tools.class).parseDateSafe(v))
    ),

    UPDATE_AT(
            "Updated At",
            "Updated At:",
            true,
            false,
            true,
            tc -> tc.getUpdatedAt().format(Config.getDateFormatterPattern()),
            null,
            (project, tc, v) -> tc.setUpdatedAt(Services.getInstance(project, Tools.class).parseDateSafe(v))
    );

    private final String name;
    private final String name2;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final boolean importValue;
    private final Function<TestCaseDto, String> valueExtractor;
    private final Function<TestCaseDto, List<JComponent>> drawItem;
    private final ImportSetter importSetter;

    public void applyToUI(final TestCaseDto tc, final List<JComponent> badges, final Map<String, String> details) {
        if (drawItem != null) badges.addAll(drawItem.apply(tc));
        else details.put(name, valueExtractor.apply(tc));
    }
}