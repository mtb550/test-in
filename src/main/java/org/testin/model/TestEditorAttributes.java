package org.testin.model;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Badges;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenType;
import org.testin.importexport.imports.ImportSetter;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Display;
import org.testin.util.NameSanitizer;
import org.testin.util.TestDataParser;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.testin.importexport.imports.ImportSetter.always;
import static org.testin.importexport.imports.ImportSetter.took;

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
            ToolBarDefault.ON,
            (tc, p) -> "",
            (p, tc, v) -> true,
            GenType.NO_CODE_CHANGE
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // Drawn by the card title, ahead of the description: "1. Log in with a valid user".
        }
    },

    DESCRIPTION(
            "Description",
            ToolBarDefault.LOCKED_CHECKED,
            (tc, p) -> tc.getDescription(),
            (p, tc, v) -> always(() -> tc.setDescription(NameSanitizer.description(v))),
            GenType.UPDATE_TEST_CASE_DESCRIPTION,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // The card title is the description; a details row under it would print it twice.
        }
    },

    ID(
            "ID",
            ToolBarDefault.LOCKED_UNCHECKED,
            (tc, p) -> String.valueOf(tc.getId()),
            (p, tc, v) -> true,
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    /**
     * Singular, and the run grid, the create dialog and the update dialog all
     * say it this way now because they ask here.
     * <p>
     * Two of them said "Expected Results" while the grid, the run editor and the
     * details panel said "Expected Result" - the same field under two names, in
     * front of the same tester, for as long as each was written out separately.
     * Nothing failed and nothing could: a caption that has already drifted does
     * not even look like a duplicated string.
     */
    EXPECTED_RESULT(
            "Expected Result",
            ToolBarDefault.ON,
            (tc, p) -> tc.getExpectedResult(),
            (p, tc, v) -> always(() -> tc.setExpectedResult(v)),
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    STEPS(
            "Steps",
            ToolBarDefault.OFF,
            (tc, p) -> String.join(", ", tc.getSteps()),
            (p, tc, v) -> always(() -> tc.setSteps(TestDataParser.steps(v))),
            GenType.UPDATE_TEST_CASE_STEPS,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    PRIORITY(
            "Priority",
            ToolBarDefault.ON,
            (tc, p) -> tc.getPriority().getLabel(),
            (p, tc, v) -> took(TestDataParser.priority(v, tc.getPriority()), tc::setPriority),
            GenType.UPDATE_TEST_CASE_PRIORITY,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            Badges.addPriorityBadge(badges, tc);
        }
    },

    FQCN(
            "FQCN",
            ToolBarDefault.OFF,
            (tc, p) -> String.join(" > ", Fqcn.ofMethod(tc)),
            (p, tc, v) -> true,
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    REFERENCE(
            "Reference",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getReference(),
            (p, tc, v) -> always(() -> tc.setReference(v)),
            GenType.NO_CODE_CHANGE,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    TEST_DATA(
            "Test Data",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getTestData(),
            (p, tc, v) -> always(() -> tc.setTestData(v)),
            GenType.UPDATE_TEST_CASE_TEST_DATA,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    PRE_CONDITIONS(
            "Pre Conditions",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getPreConditions(),
            (p, tc, v) -> always(() -> tc.setPreConditions(v)),
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    GROUP(
            "Group",
            ToolBarDefault.ON,
            (tc, p) -> tc.getGroup().stream().map(Group::getName).collect(Collectors.joining(", ")),
            (p, tc, v) -> took(TestDataParser.groups(v), tc::setGroup),
            GenType.UPDATE_TEST_CASE_GROUP,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            tc.getGroup().stream().map(Badges::createGroupBadge).forEach(badges::add);
        }
    },

    PATH(
            "Path",
            ToolBarDefault.OFF,
            (tc, p) -> String.join(" > ", tc.getParent().getPath2()),
            (p, tc, v) -> true,
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    MODULE(
            "Module",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getModule(),
            (p, tc, v) -> always(() -> tc.setModule(v)),
            GenType.UPDATE_TEST_CASE_MODULE,
            Can.EDIT, Can.IMPORT, Can.EXPORT
    ),

    STATUS(
            "Status",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getStatus().getLabel(),
            (p, tc, v) -> took(TestDataParser.testCaseStatus(v, tc.getStatus()), tc::setStatus),
            GenType.UPDATE_TEST_CASE_STATUS,
            Can.EDIT, Can.EXPORT
    ),

    CREATE_BY(
            "Created By",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getCreatedBy(),
            (p, tc, v) -> always(() -> tc.setCreatedBy(v)),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATE_BY(
            "Updated By",
            ToolBarDefault.OFF,
            (tc, p) -> tc.getUpdatedBy(),
            (p, tc, v) -> always(() -> tc.setUpdatedBy(v)),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    CREATE_AT(
            "Created At",
            ToolBarDefault.OFF,
            (tc, p) -> Display.formatDate(tc.getCreatedAt()),
            (p, tc, v) -> took(TestDataParser.date(v), tc::setCreatedAt),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATE_AT(
            "Updated At",
            ToolBarDefault.OFF,
            (tc, p) -> Display.formatDate(tc.getUpdatedAt()),
            (p, tc, v) -> took(TestDataParser.date(v), tc::setUpdatedAt),
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

    /**
     * Rule-VIEW-PANEL-026, Rule-EDITOR-PANEL-005.
     * <p>
     * The attributes a tester writes as sentences, and the only ones a reader
     * capitalizes and closes with a period.
     * <p>
     * Reference, module and test data are not on the list and must not be. A
     * reference is an identifier, a module is a label, and test data is a value
     * that gets used rather than read - a period after "JIRA-123" reads as a
     * typo, and a period after a password breaks it. The panel added one to the
     * first two for as long as each row decided formatting for itself (#22).
     * <p>
     * Here rather than at the surfaces, because the card, the details panel and
     * light mode all show the same field and each was deciding separately: the
     * card printed the raw expected result directly beside a panel showing it
     * closed with a period.
     */
    private static final @NotNull Set<TestEditorAttributes> PROSE =
            EnumSet.of(DESCRIPTION, EXPECTED_RESULT, STEPS, PRE_CONDITIONS);

    private final @NotNull String name;
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

    TestEditorAttributes(final @NotNull String name, final @NotNull ToolBarDefault toolBarDefault, final @NotNull ValueExtractor<TestCaseDto> testValueExtractor, final @NotNull ImportSetter importSetter, final @NotNull GenType genType, final @NotNull Can... can) {
        this.name = name;
        this.toolBarDefault = toolBarDefault;
        this.testValueExtractor = testValueExtractor;
        this.importSetter = importSetter;
        this.genType = genType;
        this.can = can.length == 0 ? EnumSet.noneOf(Can.class) : EnumSet.copyOf(List.of(can));
    }

    /**
     * UC-SHARE-006, Rule-SHARE-106, Rule-EDITOR-PANEL-206.
     * <p>
     * Writes one row of an imported sheet onto a test case, and answers how many
     * of its values Testin could not read.
     * <p>
     * The two importers had this loop each, differing only in how a cell is
     * fetched out of the file - so the count #264 asks for would have been
     * written twice, and the next importer would have written it a third time.
     * What varies is the cell lookup, so that is what is passed.
     */
    public static int importRow(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull Function<TestEditorAttributes, String> cell) {
        int refused = 0;

        for (final TestEditorAttributes attr : values()) {
            if (!attr.can(Can.IMPORT)) continue;
            if (!attr.importSetter.execute(p, tc, cell.apply(attr))) refused++;
        }

        return refused;
    }

    /**
     * UC-SHARE-006, Rule-SHARE-106, Rule-EDITOR-PANEL-206.
     * <p>
     * Says once how many values a sheet carried that Testin could not read, and
     * nothing at all when it could read them all.
     * <p>
     * Once with a count, never once per row: an import of two hundred cases with
     * an unreadable priority column is one thing that happened, and two hundred
     * balloons is how a tester learns to dismiss all of them (#62).
     */
    public static void sayWhatWasRefused(final @NotNull Project p, final int refused) {
        if (refused == 0) return;

        Services.getInstance(p, Notifier.class).softRefuse(p, Refused.UNREADABLE, refused + (refused == 1 ? " value" : " values"));
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
     * Rule-VIEW-PANEL-026, Rule-EDITOR-PANEL-005.
     * <p>
     * The value as a reader sees it: a sentence where the tester wrote one, and
     * untouched everywhere else.
     * <p>
     * Display only, and deliberately not what {@link #gridValue} answers. A grid
     * cell and an editor field are typed into, so they load the raw value - a
     * period this method adds would otherwise be committed back into the JSON
     * the first time a tester edited a cell they had not changed, which is the
     * whole thing #22 exists to prevent.
     */
    public @NotNull String displayValue(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull String raw = testValueExtractor.execute(tc, p);

        return PROSE.contains(this) ? Display.format(raw) : raw;
    }

    /**
     * Renders as a plain detail row. The attributes drawn as badges override
     * this in their own body — the two behaviors sit on the constants that
     * have them instead of being chosen by a null at run time.
     */
    public void applyToUI(final @NotNull TestCaseDto tc, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
        details.put(name, displayValue(p, tc));
    }

}
