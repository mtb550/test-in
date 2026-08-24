package org.testin.model;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenType;
import org.testin.editor.Shared;
import org.testin.importexport.imports.ImportSetter;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Display;
import org.testin.util.NameSanitizer;
import org.testin.util.TestDataParser;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
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
            (tc, p) -> "",
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // Drawn by the card title, ahead of the description: "1. Log in with a valid user".
        }
    },

    DESCRIPTION(
            "Description",
            "Description:",
            ToolBarDefault.LOCKED_CHECKED,
            (tc, p) -> tc.getDescription(),
            (p, tc, v) -> tc.setDescription(NameSanitizer.description(v)),
            GenType.UPDATE_TEST_CASE_DESCRIPTION,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // The card title is the description; a details row under it would print it twice.
        }
    },

    ID(
            "ID",
            "ID:",
            ToolBarDefault.LOCKED_UNCHECKED,
            (tc, p) -> String.valueOf(tc.getId()),
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    EXPECTED_RESULT(
            "Expected Result",
            "Expected Result:",
            ToolBarDefault.ON,
            (tc, p) -> tc.getExpectedResult(),
            (p, tc, v) -> tc.setExpectedResult(v),
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    STEPS(
            "Steps",
            "Steps:",
            ToolBarDefault.OFF,
            (tc, p) -> String.join(", ", tc.getSteps()),
            (p, tc, v) -> tc.setSteps(TestDataParser.steps(v)),
            GenType.UPDATE_TEST_CASE_STEPS,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    PRIORITY(
            "Priority",
            "Priority:",
            ToolBarDefault.ON,
            (tc, p) -> tc.getPriority().getName(),
            (p, tc, v) -> tc.setPriority(TestDataParser.priority(v)),
            GenType.UPDATE_TEST_CASE_PRIORITY,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            badges.add(Shared.createPriorityBadge(tc));
        }
    },

    FQCN(
            "FQCN",
            "FQCN:",
            ToolBarDefault.LOCKED_UNCHECKED,
            (tc, p) -> String.join(" > ", Fqcn.ofMethod(tc)),
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    REFERENCE(
            "Reference",
            "Reference:",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getReference(),
            (p, tc, v) -> tc.setReference(v),
            GenType.NO_CODE_CHANGE,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    TEST_DATA(
            "Test Data",
            "Test Data:",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getTestData(),
            (p, tc, v) -> tc.setTestData(v),
            GenType.UPDATE_TEST_CASE_TEST_DATA,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            "Pre Conditions:",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getPreConditions(),
            (p, tc, v) -> tc.setPreConditions(v),
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    GROUP(
            "Group",
            "Group:",
            ToolBarDefault.ON,
            (tc, p) -> tc.getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            (p, tc, v) -> tc.setGroup(TestDataParser.groups(v)),
            GenType.UPDATE_TEST_CASE_GROUP,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            tc.getGroup().stream().map(Shared::createGroupBadge).forEach(badges::add);
        }
    },

    PATH(
            "Path",
            "Path:",
            ToolBarDefault.LOCKED_UNCHECKED,
            (tc, p) -> String.join(" > ", tc.getParent().getPath2()),
            (p, tc, v) -> {
            },
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    MODULE(
            "Module",
            "Module:",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getModule(),
            (p, tc, v) -> tc.setModule(v),
            GenType.UPDATE_TEST_CASE_MODULE,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    STATUS(
            "Status",
            "Status:",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getStatus().getDisplayText(),
            (p, tc, v) -> tc.setStatus(TestCaseStatus.valueOf(v)),
            GenType.NO_CODE_CHANGE,
            Can.EDIT, Can.EXPORT
    ),

    CREATE_BY(
            "Created By",
            "Created By:",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getCreatedBy(),
            (p, tc, v) -> tc.setCreatedBy(v),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATE_BY(
            "Updated By",
            "Updated By:",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getUpdatedBy(),
            (p, tc, v) -> tc.setUpdatedBy(v),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    CREATE_AT(
            "Created At",
            "Created At:",
            ToolBarDefault.OFF,
            (tc, p) -> Display.formatDate(tc.getCreatedAt()),
            (p, tc, v) -> tc.setCreatedAt(TestDataParser.date(v)),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATE_AT(
            "Updated At",
            "Updated At:",
            ToolBarDefault.OFF,
            (tc, p) -> Display.formatDate(tc.getUpdatedAt()),
            (p, tc, v) -> tc.setUpdatedAt(TestDataParser.date(v)),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    );

    /**
     * What a tester is allowed to do with an attribute, said by name.
     * <p>
     * These were four booleans in a row on every constant - {@code true, true,
     * false, true} - and nobody could read which was which without scrolling to
     * the field declarations. A fifth thing a tester might do would have meant
     * editing all eighteen; now it means one constant here and nothing else.
     */
    public enum Can {

        /** Typed into a grid cell. False for what the tester does not own: the
         * row number, the identity a case is filed under, and the audit pairs. */
        EDIT,

        /** Read from an imported sheet. */
        IMPORT,

        /** Carried by a clipboard copy of the test case. */
        COPY,

        /** Written into an exported sheet. */
        EXPORT
    }

    private final @NotNull String name;
    private final @NotNull String name2;
    private final @NotNull ToolBarDefault toolBarDefault;

    /** How the value is read off a test case, for every surface that shows it. */
    private final @NotNull ValueExtractor<TestCaseDto> testValueExtractor;

    /** How an imported cell is written back onto a test case. */
    private final @NotNull ImportSetter importSetter;

    /** Automation code update to run when this attribute changes. */
    private final @NotNull GenType genType;

    /**
     * Empty for an attribute the tester only reads - the row number is the one.
     */
    @Getter(AccessLevel.NONE)
    private final @NotNull Set<Can> can;

    TestEditorAttributes(final @NotNull String name, final @NotNull String name2, final @NotNull ToolBarDefault toolBarDefault, final @NotNull ValueExtractor<TestCaseDto> testValueExtractor, final @NotNull ImportSetter importSetter, final @NotNull GenType genType, final @NotNull Can... can) {
        this.name = name;
        this.name2 = name2;
        this.toolBarDefault = toolBarDefault;
        this.testValueExtractor = testValueExtractor;
        this.importSetter = importSetter;
        this.genType = genType;
        this.can = can.length == 0 ? EnumSet.noneOf(Can.class) : EnumSet.copyOf(List.of(can));
    }

    /**
     * The one question every surface asks. One method rather than one accessor
     * per capability, so the grid, the import, the clipboard and the export all
     * ask in the same words - and a capability added later needs no new method.
     */
    public boolean can(final @NotNull Can capability) {
        return can.contains(capability);
    }

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
    public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
        details.put(name, testValueExtractor.execute(tc, p));
    }

}
