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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.model.BugIssueUrl;
import org.testin.model.Groups;
import org.testin.model.RunValueSetter;
import org.testin.model.TestRunItems;
import org.testin.model.ToolBarAttribute;
import org.testin.model.ToolBarDefault;
import org.testin.testcase.TestEditorAttributes;
import org.testin.ui.Badges;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum RunEditorAttributes implements ToolBarAttribute {
    ORDER(
            TestEditorAttributes.ORDER.getName(),
            ToolBarDefault.LOCKED_CHECKED,
            item -> ""
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    DESCRIPTION(
            TestEditorAttributes.DESCRIPTION.getName(),
            ToolBarDefault.ON,
            item -> item.shownCase().getDescription()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    EXPECTED_RESULT(
            TestEditorAttributes.EXPECTED_RESULT.getName(),
            ToolBarDefault.ON,
            item -> item.shownCase().getExpectedResult()
    ),

    STEPS(
            TestEditorAttributes.STEPS.getName(),
            ToolBarDefault.OFF,
            item -> String.join(", ", item.shownCase().getSteps())
    ),

    PRIORITY(
            TestEditorAttributes.PRIORITY.getName(),
            ToolBarDefault.OFF,
            item -> item.shownCase().getPriority().getLabel()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addPriorityBadge(badges, runItem.shownCase());
        }
    },

    GROUP(
            TestEditorAttributes.GROUP.getName(),
            ToolBarDefault.OFF,
            item -> Groups.text(item.shownCase().getGroup())
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
            runItem.shownCase().getGroup().stream().map(Badges::createGroupBadge).forEach(badges::add);
        }
    },

    ACTUAL_RESULT(
            Bundle.message("attribute.run.actual.result"),
            ToolBarDefault.ON,
            TestRunItems::getActualResult,
            TestRunItems::setActualResult
    ),

    STACKTRACE(
            Bundle.message("attribute.run.stacktrace"),
            ToolBarDefault.OFF,
            TestRunItems::getStacktrace
    ),

    BUG_SEVERITY(
            Bundle.message("attribute.run.bug.severity"),
            ToolBarDefault.ON,
            item -> item.getBugSeverity().getLabel()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addBugBadge(badges, runItem.getBugSeverity().getLabel(), runItem.getBugSeverity().getColor());
        }
    },

    BUG_PRIORITY(
            Bundle.message("attribute.run.bug.priority"),
            ToolBarDefault.ON,
            item -> item.getBugPriority().getLabel()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addBugBadge(badges, runItem.getBugPriority().getLabel(), runItem.getBugPriority().getColor());
        }
    },

    BUG_ISSUE(
            Bundle.message("attribute.run.bug.issue"),
            ToolBarDefault.OFF,
            item -> BugIssueUrl.reference(item.getBugIssueUrl())
    ),

    RUN_STATUS(
            Bundle.message("attribute.run.run.status"),
            ToolBarDefault.ON,
            item -> item.shownStatus().getLabel()
    ),

    DURATION(
            Bundle.message("attribute.run.duration"),
            ToolBarDefault.ON,
            item -> Display.formatDuration(item.getDuration())
    ),

    EXECUTED_BY(
            Bundle.message("attribute.run.executed.by"),
            ToolBarDefault.OFF,
            TestRunItems::getExecutedBy
    ),

    EXECUTED_AT(
            Bundle.message("attribute.run.executed.at"),
            ToolBarDefault.OFF,
            item -> Display.formatDate(item.getExecutedAt())
    ),

    PATH(
            TestEditorAttributes.PATH.getName(),
            ToolBarDefault.OFF,
            item -> String.join(" > ", item.liveCase().getParent().getPath2())
    ),

    FQCN(
            TestEditorAttributes.FQCN.getName(),
            ToolBarDefault.LOCKED_UNCHECKED,
            item -> String.join(" > ", Fqcn.ofMethod(item.liveCase()))
    );

    private final @NotNull String name;
    private final @NotNull ToolBarDefault toolBarDefault;
    private final @NotNull Function<TestRunItems, String> runValueExtractor;

    private final @NotNull RunValueSetter runValueSetter;

    RunEditorAttributes(final @NotNull String name, final @NotNull ToolBarDefault toolBarDefault, final @NotNull Function<TestRunItems, String> runValueExtractor) {
        this(name, toolBarDefault, runValueExtractor, RunValueSetter.NONE);
    }

    public boolean isEdited() {
        return runValueSetter != RunValueSetter.NONE;
    }

    public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
        details.put(name, runValueExtractor.apply(runItem));
    }
}
