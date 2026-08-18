package org.testin.model;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.editor.Shared;
import org.testin.importexport.imports.ImportSetter;
import org.testin.model.dto.TestCaseDto;
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
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE
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
            (p, tc, v) -> tc.setDescription(Services.getInstance(p, Tools.class).sanitizeDescription(v)),
            GenType.UPDATE_TEST_CASE_DESCRIPTION
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
            (p, tc, v) -> tc.setExpectedResult(v),
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT
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
            (p, tc, v) -> tc.setSteps(Services.getInstance(p, Tools.class).parseStepsSafe(v)),
            GenType.UPDATE_TEST_CASE_STEPS
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
            (p, tc, v) -> tc.setPriority(Services.getInstance(p, Tools.class).parsePrioritySafe(v)),
            GenType.UPDATE_TEST_CASE_PRIORITY
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<JComponent> badges,
                              final @NotNull Map<String, String> details, final @NotNull Project p) {
            badges.add(Shared.createPriorityBadge(tc));
        }
    },

    FQCN(
            "FQCN",
            "FQCN:",
            true,
            true,
            false,
            false,
            true,
            (tc, p) -> String.join(" > ", Services.getInstance(p, Tools.class).buildFqcnMethod(tc)),
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE
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
            (p, tc, v) -> tc.setReference(v),
            GenType.NO_CODE_CHANGE
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
            (p, tc, v) -> tc.setTestData(v),
            GenType.UPDATE_TEST_CASE_TEST_DATA
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
            (p, tc, v) -> tc.setPreConditions(v),
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS
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
            (p, tc, v) -> tc.setGroup(Services.getInstance(p, Tools.class).parseGroupsSafe(v)),
            GenType.UPDATE_TEST_CASE_GROUP
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<JComponent> badges,
                              final @NotNull Map<String, String> details, final @NotNull Project p) {
            tc.getGroup().stream().map(Shared::createGroupBadge).forEach(badges::add);
        }
    },

    PATH(
            "Path",
            "Path:",
            true,
            false,
            false,
            false,
            true,
            (tc, p) -> String.join(" > ", tc.getParent().getPath2()),
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE
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
            (p, tc, v) -> tc.setModule(v),
            GenType.UPDATE_TEST_CASE_MODULE
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
            (p, tc, v) -> tc.setStatus(TestCaseStatus.valueOf(v)),
            GenType.NO_CODE_CHANGE
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
            (p, tc, v) -> tc.setCreatedBy(v),
            GenType.NO_CODE_CHANGE
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
            (p, tc, v) -> tc.setUpdatedBy(v),
            GenType.NO_CODE_CHANGE
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
            (p, tc, v) -> tc.setCreatedAt(Services.getInstance(p, Tools.class).parseDateSafe(v)),
            GenType.NO_CODE_CHANGE
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
            (p, tc, v) -> tc.setUpdatedAt(Services.getInstance(p, Tools.class).parseDateSafe(v)),
            GenType.NO_CODE_CHANGE
    );

    private final @NotNull String name;
    private final @NotNull String name2;
    private final boolean standardToolBarOption;
    private final boolean defaultToolBarSelected;
    private final boolean importable;
    private final boolean copyable;
    private final boolean exportable;
    private final @NotNull ValueExtractor<TestCaseDto> testValueExtractor;

    private final @NotNull ImportSetter importSetter;
    /**
     * Automation code update to run when this attribute changes.
     */
    private final @NotNull GenType genType;

    /**
     * The value as the grid shows it. Steps get one line each there, so ALT+ENTER
     * writes the next step; the sequence numbers stay a view-panel concern. Every
     * other attribute - and every other surface, including exports, clipboard
     * copy and the import preview - uses the canonical extractor unchanged.
     */
    public @NotNull String gridValue(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return this == STEPS ? String.join("\n", tc.getSteps()) : testValueExtractor.execute(tc, p);
    }

    /**
     * Renders as a plain detail row. The attributes drawn as badges override
     * this in their own body — the two behaviors sit on the constants that
     * have them instead of being chosen by a null at run time.
     */
    public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<JComponent> badges,
                          final @NotNull Map<String, String> details, final @NotNull Project p) {
        details.put(name, testValueExtractor.execute(tc, p));
    }

}
