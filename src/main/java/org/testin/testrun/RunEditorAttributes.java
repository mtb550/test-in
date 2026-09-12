/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.testrun;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.Groups;
import org.testin.model.RunValueSetter;
import org.testin.model.TestRunItems;
import org.testin.model.ToolBarAttribute;
import org.testin.model.ToolBarDefault;
import org.testin.model.ValueExtractor;
import org.testin.util.Bundle;
import org.testin.ui.Badges;
import org.testin.codegen.Fqcn;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Display;

import java.util.List;
import java.util.Map;

@Getter
public enum RunEditorAttributes implements ToolBarAttribute {

    /**
     * The row's position on the page, drawn by the card title and by the grid's
     * first column. The run item carries no such value - the position is the
     * view's, not the model's - so the extractor is empty and each view fills
     * the number in from the index it is already counting.
     * <p>
     * Locked on, for the reason {@link TestEditorAttributes#ORDER} gives: the
     * run grid is built by the same builder and answers the same three gestures,
     * so unticking it here broke them here too (#207).
     */
    ORDER(
            TestEditorAttributes.ORDER.getName(),
            ToolBarDefault.LOCKED_CHECKED,
            (item, p) -> ""
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // Drawn by the card title, ahead of the description: "1. Log in with a valid user".
        }
    },

    DESCRIPTION(
            TestEditorAttributes.DESCRIPTION.getName(),
            ToolBarDefault.ON,
            (item, p) -> item.requireTc().getDescription()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // The card title is the description; a details row under it would print it twice.
        }
    },

    EXPECTED_RESULT(
            TestEditorAttributes.EXPECTED_RESULT.getName(),
            ToolBarDefault.ON,
            (item, p) -> item.requireTc().getExpectedResult()
    ),

    STEPS(
            TestEditorAttributes.STEPS.getName(),
            ToolBarDefault.OFF,
            (item, p) -> String.join(", ", item.requireTc().getSteps())
    ),

    PRIORITY(
            TestEditorAttributes.PRIORITY.getName(),
            ToolBarDefault.OFF,
            (item, p) -> item.requireTc().getPriority().getLabel()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            Badges.addPriorityBadge(badges, runItem.requireTc());
        }
    },

    GROUP(
            TestEditorAttributes.GROUP.getName(),
            ToolBarDefault.OFF,
            (item, p) -> Groups.text(item.requireTc().getGroup())
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            runItem.requireTc().getGroup().stream().map(Badges::createGroupBadge).forEach(badges::add);
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
            Bundle.message("attribute.run.actual.result"),
            ToolBarDefault.ON,
            (item, p) -> item.getActualResult(),
            TestRunItems::setActualResult
    ),

    /**
     * The framework's own account of the failure, or whatever the tester pasted
     * into the Error Capture box.
     * <p>
     * Off by default. It is the one run value measured in paragraphs rather than
     * words, so a column of it crowds out every other column - but it was absent
     * from this enum altogether until automation started filling the field on
     * its own, which left it in the run JSON, in the failure dialog, in the
     * Excel export alone, and nowhere a tester looks while executing.
     */
    STACKTRACE(
            Bundle.message("attribute.run.stacktrace"),
            ToolBarDefault.OFF,
            (item, p) -> item.getStacktrace()
    ),

    BUG_SEVERITY(
            Bundle.message("attribute.run.bug.severity"),
            ToolBarDefault.ON,
            (item, p) -> item.getBugSeverity().getLabel()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            Badges.addBugBadge(badges, runItem.getBugSeverity().getLabel(), runItem.getBugSeverity().getColor());
        }
    },

    BUG_PRIORITY(
            Bundle.message("attribute.run.bug.priority"),
            ToolBarDefault.ON,
            (item, p) -> item.getBugPriority().getLabel()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
            // Its own half of the badge severity started, or the whole of it
            // when severity is not shown. One badge either way (#89).
            Badges.addBugBadge(badges, runItem.getBugPriority().getLabel(), runItem.getBugPriority().getColor());
        }
    },

    RUN_STATUS(
            Bundle.message("attribute.run.run.status"),
            ToolBarDefault.ON,
            (item, p) -> item.getStatus().getLabel()
    ),

    DURATION(
            Bundle.message("attribute.run.duration"),
            ToolBarDefault.ON,
            (item, p) -> Display.formatDuration(item.getDuration())
    ),

    EXECUTED_BY(
            Bundle.message("attribute.run.executed.by"),
            ToolBarDefault.OFF,
            (item, p) -> item.getExecutedBy()
    ),

    EXECUTED_AT(
            Bundle.message("attribute.run.executed.at"),
            ToolBarDefault.OFF,
            (item, p) -> Display.formatDate(item.getExecutedAt())
    ),

    PATH(
            TestEditorAttributes.PATH.getName(),
            ToolBarDefault.OFF,
            (item, p) -> Services.getInstance(p, ProjectIndexer.class).findTestCase(item.getId())
                    .map(tc -> String.join(" > ", tc.getParent().getPath2()))
                    .orElse("")
    ),

    FQCN(
            TestEditorAttributes.FQCN.getName(),
            ToolBarDefault.LOCKED_UNCHECKED,
            (item, p) -> {
                final @NotNull TestCaseDto tc = item.requireTc();
                return String.join(" > ", Fqcn.ofMethod(tc));
            }
    );

    private final @NotNull String name;
    private final @NotNull ToolBarDefault toolBarDefault;
    private final @NotNull ValueExtractor runValueExtractor;

    /**
     * What typing into this column does. {@link RunValueSetter#NONE} for every
     * column that is only read.
     */
    private final @NotNull RunValueSetter runValueSetter;

    RunEditorAttributes(final @NotNull String name, final @NotNull ToolBarDefault toolBarDefault, final @NotNull ValueExtractor runValueExtractor) {
        this(name, toolBarDefault, runValueExtractor, RunValueSetter.NONE);
    }

    RunEditorAttributes(final @NotNull String name, final @NotNull ToolBarDefault toolBarDefault, final @NotNull ValueExtractor runValueExtractor, final @NotNull RunValueSetter runValueSetter) {
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
    public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details, final @NotNull Project p) {
        details.put(name, runValueExtractor.execute(runItem, p));
    }

}
