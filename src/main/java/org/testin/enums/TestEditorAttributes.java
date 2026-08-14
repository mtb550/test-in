package org.testin.enums;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.GeneratorType;
import org.testin.editorPanel.Shared;
import org.testin.importExport.imports.ImportSetter;
import org.testin.mappers.Config;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: add order, then add it toolbar details (select by the order number) & add it to edit menu.
// TODO: add all to edit menu: auto ref, business ref..etc
// TODO: also, map all to view panel dynamically.
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
            (tc, p) -> String.valueOf(tc.getId()),
            null,
            (p, tc, v) -> {
            },
            null
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
            (tc, p) -> tc.getDescription(),
            null,
            (p, tc, v) -> tc.setDescription(Services.getInstance(p, Tools.class).sanitizeDescription(v)),
            GeneratorType.UPDATE_TEST_CASE_DESCRIPTION
    ),

    EXPECTED_RESULT(
            "Expected Result",
            "Expected Result:",
            true,
            true,
            true,
            false,
            true,
            (tc, p) -> tc.getExpectedResult(),
            null,
            (p, tc, v) -> tc.setExpectedResult(v),
            GeneratorType.UPDATE_TEST_CASE_EXPECTED_RESULT
    ),

    STEPS(
            "Steps",
            "Steps:",
            true,
            true,
            true,
            false,
            true,
            (tc, p) -> String.join(", ", tc.getSteps()),
            null,
            (p, tc, v) -> tc.setSteps(Services.getInstance(p, Tools.class).parseStepsSafe(v)),
            GeneratorType.UPDATE_TEST_CASE_STEPS
    ),

    PRIORITY(
            "Priority",
            "Priority:",
            true,
            true,
            true,
            false,
            true,
            (tc, p) -> tc.getPriority().getName(),
            tc -> List.of(Shared.createPriorityBadge(tc)),
            (p, tc, v) -> tc.setPriority(Services.getInstance(p, Tools.class).parsePrioritySafe(v)),
            GeneratorType.UPDATE_TEST_CASE_PRIORITY
    ),

    FQCN(
            "FQCN",
            "FQCN:",
            true,
            true,
            false,
            false,
            true,
            (tc, p) -> String.join(" > ", Services.getInstance(p, Tools.class).buildFqcnMethod(tc)),
            null,
            (p, tc, v) -> {
            },
            null
    ),

    REFERENCE(
            "Reference",
            "Reference:",
            true,
            false,
            true,
            false,
            true,
            (tc, p) -> tc.getReference(),
            null,
            (p, tc, v) -> tc.setReference(v),
            null
    ),

    TEST_DATA(
            "Test Data",
            "Test Data:",
            true,
            false,
            true,
            false,
            true,
            (tc, p) -> tc.getTestData(),
            null,
            (p, tc, v) -> tc.setTestData(v),
            GeneratorType.UPDATE_TEST_CASE_TEST_DATA
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            "Pre Conditions:",
            true,
            false,
            true,
            false,
            true,
            (tc, p) -> tc.getPreConditions(),
            null,
            (p, tc, v) -> tc.setPreConditions(v),
            GeneratorType.UPDATE_TEST_CASE_PRE_CONDITIONS
    ),

    GROUP(
            "Group",
            "Group:",
            true,
            true,
            true,
            false,
            true,
            (tc, p) -> tc.getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            tc -> tc.getGroup().stream().map(Shared::createGroupBadge).collect(Collectors.<JComponent>toList()),
            (p, tc, v) -> tc.setGroup(Services.getInstance(p, Tools.class).parseGroupsSafe(v)),
            GeneratorType.UPDATE_TEST_CASE_GROUP
    ),

    PATH(
            "Path",
            "Path:",
            true,
            false,
            false,
            false,
            true,
            (tc, p) -> String.join(" > ", tc.getParent().getPath2()),
            null,
            (p, tc, v) -> {
            },
            null
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
            (tc, p) -> tc.getModule(),
            null,
            (p, tc, v) -> tc.setModule(v),
            GeneratorType.UPDATE_TEST_CASE_MODULE
    ),

    STATUS(
            "Status",
            "Status:",
            true,
            false,
            false,
            false,
            true,
            (tc, p) -> tc.getStatus().getDisplayText(),
            null,
            (p, tc, v) -> tc.setStatus(TestCaseStatus.valueOf(v)),
            null
    ),

    CREATE_BY(
            "Created By",
            "Created By:",
            true,
            false,
            true,
            false,
            true,
            (tc, p) -> tc.getCreatedBy(),
            null,
            (p, tc, v) -> tc.setCreatedBy(v),
            null
    ),

    UPDATE_BY(
            "Updated By",
            "Updated By:",
            true,
            false,
            true,
            false,
            true,
            (tc, p) -> tc.getUpdatedBy(),
            null,
            (p, tc, v) -> tc.setUpdatedBy(v),
            null
    ),

    CREATE_AT(
            "Created At",
            "Created At:",
            true,
            false,
            true,
            false,
            true,
            (tc, p) -> tc.getCreatedAt().format(Config.getDateFormatterPattern()),
            null,
            (p, tc, v) -> tc.setCreatedAt(Services.getInstance(p, Tools.class).parseDateSafe(v)),
            null
    ),

    UPDATE_AT(
            "Updated At",
            "Updated At:",
            true,
            false,
            true,
            false,
            true,
            (tc, p) -> tc.getUpdatedAt().format(Config.getDateFormatterPattern()),
            null,
            (p, tc, v) -> tc.setUpdatedAt(Services.getInstance(p, Tools.class).parseDateSafe(v)),
            null
    );

    private final @NotNull String name;
    private final @NotNull String name2;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final boolean importable;
    private final boolean copyable;
    private final boolean exportable;
    private final @NotNull ValueExtractor<TestCaseDto> testValueExtractor;

    /**
     * Null for attributes shown as a plain detail row rather than a badge.
     */
    private final @Nullable DrawItem<TestCaseDto> testDrawItem;
    private final @NotNull ImportSetter importSetter;
    /**
     * Automation code update to run when this attribute changes; null when the
     * attribute has no effect on the generated Java code.
     */
    private final @Nullable GeneratorType generatorType;

    /**
     * The value as the grid shows it. Steps get one line each there, so ALT+ENTER
     * writes the next step; the sequence numbers stay a view-panel concern. Every
     * other attribute - and every other surface, including exports, clipboard
     * copy and the import preview - uses the canonical extractor unchanged.
     */
    public @NotNull String gridValue(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return this == STEPS ? String.join("\n", tc.getSteps()) : testValueExtractor.execute(tc, p);
    }

    public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<JComponent> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
        if (testDrawItem != null) badges.addAll(testDrawItem.execute(tc));
        else details.put(name, testValueExtractor.execute(tc, p));
    }

}
