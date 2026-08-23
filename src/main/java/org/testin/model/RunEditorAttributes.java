package org.testin.model;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.editor.Shared;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Display;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum RunEditorAttributes implements ToolBarAttribute {

    /**
     * The row's position on the page, drawn by the card title and by the grid's
     * first column. The run item carries no such value - the position is the
     * view's, not the model's - so the extractor is empty and each view fills
     * the number in from the index it is already counting.
     */
    ORDER(
            "Order",
            ToolBarDefault.ON,
            (item, p) -> ""
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // Drawn by the card title, ahead of the description: "1. Log in with a valid user".
        }
    },

    DESCRIPTION(
            "Description",
            ToolBarDefault.ON,
            (item, p) -> item.requireTc().getDescription()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // The card title is the description; a details row under it would print it twice.
        }
    },

    EXPECTED_RESULT(
            "Expected Result",
            ToolBarDefault.ON,
            (item, p) -> item.requireTc().getExpectedResult()
    ),

    STEPS(
            "Steps",
            ToolBarDefault.OFF,
            (item, p) -> String.join(", ", item.requireTc().getSteps())
    ),

    PRIORITY(
            "Priority",
            ToolBarDefault.OFF,
            (item, p) -> item.requireTc().getPriority().getName()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            badges.add(Shared.createPriorityBadge(runItem.requireTc()));
        }
    },

    GROUP(
            "Group",
            ToolBarDefault.OFF,
            (item, p) -> item.requireTc().getGroup().stream().map(Group::getName).collect(Collectors.joining(", "))
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            runItem.requireTc().getGroup().stream().map(Shared::createGroupBadge).forEach(badges::add);
        }
    },

    /**
     * The one column of a run grid a tester types into (#74).
     * <p>
     * Everything else on a row is either the test case's, which the run does not
     * own, or a verdict, which is set by its own key and clears the fields that
     * explain a failure as it goes. Typing a status into a cell would be a
     * fourth way to record one.
     */
    ACTUAL_RESULT(
            "Actual Result",
            ToolBarDefault.ON,
            (item, p) -> item.getActualResult(),
            TestRunItems::setActualResult
    ),

    BUG_SEVERITY(
            "Bug Severity",
            ToolBarDefault.ON,
            (item, p) -> item.getBugSeverity().getName()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            Shared.addBadge(badges, getName(), runItem.getBugSeverity().getName(), runItem.getBugSeverity().getColor());
        }
    },

    BUG_PRIORITY(
            "Bug Priority",
            ToolBarDefault.ON,
            (item, p) -> item.getBugPriority().getName()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            Shared.addBadge(badges, getName(), runItem.getBugPriority().getName(), runItem.getBugPriority().getColor());
        }
    },

    RUN_STATUS(
            "Run Status",
            ToolBarDefault.ON,
            (item, p) -> item.getStatus().getDisplayText()
    ),

    DURATION(
            "Duration",
            ToolBarDefault.ON,
            (item, p) -> Display.formatDuration(item.getDuration())
    ),

    EXECUTED_BY(
            "Executed By",
            ToolBarDefault.OFF,
            (item, p) -> item.getExecutedBy()
    ),

    EXECUTED_AT(
            "Executed At",
            ToolBarDefault.OFF,
            (item, p) -> Display.formatDate(item.getExecutedAt())
    ),

    PATH(
            "Path",
            ToolBarDefault.LOCKED_UNCHECKED,
            (item, p) -> Services.getInstance(p, ProjectIndexer.class).findTestCase(item.getId())
                    .map(tc -> String.join(" > ", tc.getParent().getPath2()))
                    .orElse("")
    ),

    FQCN(
            "FQCN",
            ToolBarDefault.LOCKED_UNCHECKED,
            (item, p) -> {
                final @NotNull TestCaseDto tc = item.requireTc();
                return String.join(" > ", Fqcn.ofMethod(tc));
            }
    );

    private final @NotNull String name;
    private final @NotNull ToolBarDefault toolBarDefault;
    private final @NotNull ValueExtractor<TestRunItems> runValueExtractor;

    /**
     * What typing into this column does. {@link RunValueSetter#NONE} for every
     * column that is only read.
     */
    private final @NotNull RunValueSetter runValueSetter;

    RunEditorAttributes(final @NotNull String name, final @NotNull ToolBarDefault toolBarDefault,
                        final @NotNull ValueExtractor<TestRunItems> runValueExtractor) {
        this(name, toolBarDefault, runValueExtractor, RunValueSetter.NONE);
    }

    RunEditorAttributes(final @NotNull String name, final @NotNull ToolBarDefault toolBarDefault,
                        final @NotNull ValueExtractor<TestRunItems> runValueExtractor,
                        final @NotNull RunValueSetter runValueSetter) {
        this.name = name;
        this.toolBarDefault = toolBarDefault;
        this.runValueExtractor = runValueExtractor;
        this.runValueSetter = runValueSetter;
    }

    /**
     * Whether the grid lets a tester type into this column.
     * <p>
     * The one place that asks, so the table model, the edit listener and
     * anything added later all get the same answer from the same declaration -
     * rather than the model refusing one set of columns and a listener guarding
     * a different set.
     */
    public boolean isEdited() {
        return runValueSetter != RunValueSetter.NONE;
    }

    /**
     * Renders as a plain detail row. The attributes drawn as badges override
     * this in their own body — the two behaviors sit on the constants that
     * have them instead of being chosen by a null at run time.
     */
    public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Shared.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
        details.put(name, runValueExtractor.execute(runItem, p));
    }

}
