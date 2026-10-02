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
import org.testin.model.RunItemValueSetter;
import org.testin.model.TestRunItems;
import org.testin.model.ToolBarAttribute;
import org.testin.model.ToolBarDefault;
import org.testin.testcase.TestCaseEditorAttributes;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum TestRunEditorAttributes implements ToolBarAttribute {
    ORDER(
            TestCaseEditorAttributes.ORDER.getName(),
            ToolBarDefault.LOCKED_CHECKED,
            _ -> ""
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    DESCRIPTION(
            TestCaseEditorAttributes.DESCRIPTION.getName(),
            ToolBarDefault.ON,
            item -> item.shownTestCase().getDescription()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    EXPECTED_RESULT(
            TestCaseEditorAttributes.EXPECTED_RESULT.getName(),
            ToolBarDefault.ON,
            item -> item.shownTestCase().getExpectedResult()
    ),

    STEPS(
            TestCaseEditorAttributes.STEPS.getName(),
            ToolBarDefault.OFF,
            item -> String.join(", ", item.shownTestCase().getSteps())
    ),

    PRIORITY(
            TestCaseEditorAttributes.PRIORITY.getName(),
            ToolBarDefault.OFF,
            item -> item.shownTestCase().getPriority().getLabel()
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addPriorityBadge(badges, runItem.shownTestCase());
        }
    },

    GROUP(
            TestCaseEditorAttributes.GROUP.getName(),
            ToolBarDefault.OFF,
            item -> Groups.text(item.shownTestCase().getGroup())
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            runItem.shownTestCase().getGroup().stream().map(Badges::createGroupBadge).forEach(badges::add);
        }
    },

    ACTUAL_RESULT(
            Bundle.message("attribute.run.item.actual.result"),
            ToolBarDefault.ON,
            TestRunItems::getActualResult,
            TestRunItems::recordActualResult
    ),

    STACKTRACE(
            Bundle.message("attribute.run.item.stacktrace"),
            ToolBarDefault.OFF,
            TestRunItems::getStacktrace
    ),

    BUG_SEVERITY(
            Bundle.message("attribute.run.item.bug.severity"),
            ToolBarDefault.ON,
            item -> item.isFailed() ? item.getBugSeverity().getLabel() : ""
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addBugBadge(badges, getRunItemValueExtractor().apply(runItem), runItem.getBugSeverity().getColor());
        }
    },

    BUG_PRIORITY(
            Bundle.message("attribute.run.item.bug.priority"),
            ToolBarDefault.ON,
            item -> item.isFailed() ? item.getBugPriority().getLabel() : ""
    ) {
        @Override
        public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addBugBadge(badges, getRunItemValueExtractor().apply(runItem), runItem.getBugPriority().getColor());
        }
    },

    BUG_ISSUE(
            Bundle.message("attribute.run.item.bug.issue"),
            ToolBarDefault.OFF,
            item -> BugIssueUrl.reference(item.getBugIssueUrl())
    ),

    RUN_STATUS(
            Bundle.message("attribute.run.item.status"),
            ToolBarDefault.ON,
            item -> item.shownStatus().getLabel()
    ),

    DURATION(
            Bundle.message("attribute.run.item.duration"),
            ToolBarDefault.ON,
            item -> Display.formatDuration(item.getDuration())
    ),

    EXECUTED_BY(
            Bundle.message("attribute.run.item.executed.by"),
            ToolBarDefault.OFF,
            TestRunItems::getExecutedBy
    ),

    EXECUTED_AT(
            Bundle.message("attribute.run.item.executed.at"),
            ToolBarDefault.OFF,
            item -> Display.formatDate(item.getExecutedAt())
    ),

    PATH(
            TestCaseEditorAttributes.PATH.getName(),
            ToolBarDefault.OFF,
            item -> String.join(" > ", item.liveTestCase().getParent().getPath2())
    ),

    FQCN(
            TestCaseEditorAttributes.FQCN.getName(),
            ToolBarDefault.LOCKED_UNCHECKED,
            item -> String.join(" > ", Fqcn.ofMethod(item.liveTestCase()))
    );

    private final @NotNull String name;
    private final @NotNull ToolBarDefault toolBarDefault;
    private final @NotNull Function<TestRunItems, String> runItemValueExtractor;

    private final @NotNull RunItemValueSetter runItemValueSetter;

    TestRunEditorAttributes(final @NotNull String name, final @NotNull ToolBarDefault toolBarDefault, final @NotNull Function<TestRunItems, String> runItemValueExtractor) {
        this(name, toolBarDefault, runItemValueExtractor, RunItemValueSetter.NONE);
    }

    public boolean isEdited() {
        return runItemValueSetter != RunItemValueSetter.NONE;
    }

    public void applyToUI(final @NotNull TestRunItems runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        details.put(name, runItemValueExtractor.apply(runItem));
    }
}
