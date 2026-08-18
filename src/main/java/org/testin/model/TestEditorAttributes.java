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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum TestEditorAttributes implements ToolBarAttribute {

    /**
     * The row's position on the page, drawn by the card title and by the grid's
     * first column. The test case carries no such value - the position is the
     * view's, not the model's - so the extractor is empty and each view fills
     * the number in from the index it is already counting.
     */
    ORDER(
            "Order",
            "Order:",
            ToolBarDefault.ON,
            false,
            false,
            false,
            false,
            (tc, p) -> "",
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges,
                              final @NotNull Map<String, String> details, final @NotNull Project p) {
            // Drawn by the card title, ahead of the description: "1. Log in with a valid user".
        }
    },

    DESCRIPTION(
            "Description",
            "Description:",
            ToolBarDefault.LOCKED_CHECKED,
            true,
            true,
            true,
            true,
            (tc, p) -> tc.getDescription(),
            (p, tc, v) -> tc.setDescription(Services.getInstance(p, Tools.class).sanitizeDescription(v)),
            GenType.UPDATE_TEST_CASE_DESCRIPTION
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges,
                              final @NotNull Map<String, String> details, final @NotNull Project p) {
            // The card title is the description; a details row under it would print it twice.
        }
    },

    ID(
            "ID",
            "ID:",
            ToolBarDefault.LOCKED_UNCHECKED,
            false,
            false,
            false,
            true,
            (tc, p) -> String.valueOf(tc.getId()),
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE
    ),

    EXPECTED_RESULT(
            "Expected Result",
            "Expected Result:",
            ToolBarDefault.ON,
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
            ToolBarDefault.OFF,
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
            ToolBarDefault.ON,
            true,
            true,
            false,
            true,
            (tc, p) -> tc.getPriority().getName(),
            (p, tc, v) -> tc.setPriority(Services.getInstance(p, Tools.class).parsePrioritySafe(v)),
            GenType.UPDATE_TEST_CASE_PRIORITY
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges,
                              final @NotNull Map<String, String> details, final @NotNull Project p) {
            badges.add(Shared.createPriorityBadge(tc));
        }
    },

    FQCN(
            "FQCN",
            "FQCN:",
            ToolBarDefault.LOCKED_UNCHECKED,
            false,
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
            ToolBarDefault.OFF,
            true,
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
            ToolBarDefault.OFF,
            true,
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
            ToolBarDefault.OFF,
            true,
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
            ToolBarDefault.ON,
            true,
            true,
            false,
            true,
            (tc, p) -> tc.getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            (p, tc, v) -> tc.setGroup(Services.getInstance(p, Tools.class).parseGroupsSafe(v)),
            GenType.UPDATE_TEST_CASE_GROUP
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges,
                              final @NotNull Map<String, String> details, final @NotNull Project p) {
            tc.getGroup().stream().map(Shared::createGroupBadge).forEach(badges::add);
        }
    },

    PATH(
            "Path",
            "Path:",
            ToolBarDefault.LOCKED_UNCHECKED,
            false,
            false,
            false,
            true,
            (tc, p) -> String.join(" > ", tc.getParent().getPath2()),
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE
    ),

    MODULE(
            "Module",
            "Module:",
            ToolBarDefault.OFF,
            true,
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
            ToolBarDefault.OFF,
            true,
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
            ToolBarDefault.OFF,
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
            ToolBarDefault.OFF,
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
            ToolBarDefault.OFF,
            false,
            true,
            false,
            true,
            (tc, p) -> Config.formatOrBlank(tc.getCreatedAt()),
            (p, tc, v) -> tc.setCreatedAt(Services.getInstance(p, Tools.class).parseDateSafe(v)),
            GenType.NO_CODE_CHANGE
    ),

    UPDATE_AT(
            "Updated At",
            "Updated At:",
            ToolBarDefault.OFF,
            false,
            true,
            false,
            true,
            (tc, p) -> Config.formatOrBlank(tc.getUpdatedAt()),
            (p, tc, v) -> tc.setUpdatedAt(Services.getInstance(p, Tools.class).parseDateSafe(v)),
            GenType.NO_CODE_CHANGE
    );

    private final @NotNull String name;
    private final @NotNull String name2;
    private final @NotNull ToolBarDefault toolBarDefault;

    /**
     * Whether a tester may type this into a grid cell. False for what the tester
     * does not own - the row number, the identity a test case is filed under, and
     * the audit pairs, which the save path fills in. The grid asks the attribute
     * rather than the column number, so a column that cannot be written never
     * opens an editor to begin with.
     */
    private final boolean editable;
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
    public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges,
                          final @NotNull Map<String, String> details, final @NotNull Project p) {
        details.put(name, testValueExtractor.execute(tc, p));
    }

}
