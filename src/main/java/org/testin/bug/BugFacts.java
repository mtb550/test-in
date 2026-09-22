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

package org.testin.bug;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.report.generators.ReportText;
import org.testin.testcase.TestEditorAttributes;
import org.testin.util.Display;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record BugFacts(@NotNull String title, @NotNull BugSeverity severity, @NotNull BugPriority priority, @NotNull String platform, @NotNull String actualResult, @NotNull String expectedResult, @NotNull List<String> steps, @NotNull String testData, @NotNull String stacktrace, @NotNull List<byte[]> screenshots, @NotNull String testRun, @NotNull String executed, @NotNull String browser, @NotNull String device, @NotNull String language, @NotNull String commit, @NotNull UUID testCaseId, @NotNull String testSetName) {
    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-068
    public static @NotNull BugFacts of(final @NotNull TestRunItems item, final @NotNull TestCaseDto tc, final @NotNull TestRunMarker run, final @NotNull String testRun, final @NotNull List<byte[]> screenshots) {
        return new BugFacts(
                TestEditorAttributes.DESCRIPTION.displayValue(tc),
                item.getBugSeverity(),
                item.getBugPriority(),
                ReportText.joined(BugTemplate.SEPARATOR, TestRunConfiguration.PLATFORM.valueIn(run), TestRunConfiguration.COMPONENT.valueIn(run)),
                item.getActualResult(),
                TestEditorAttributes.EXPECTED_RESULT.displayValue(tc),
                tc.getSteps().stream().map(Display::format).toList(),
                tc.getTestData(),
                item.getStacktrace(),
                screenshots,
                testRun,
                ReportText.joined(BugTemplate.SEPARATOR, item.getExecutedBy(), Display.formatDate(item.getExecutedAt())),
                TestRunConfiguration.BROWSER.valueIn(run),
                TestRunConfiguration.DEVICE_TYPE.valueIn(run),
                TestRunConfiguration.LANGUAGE.valueIn(run),
                TestRunConfiguration.COMMIT_ID.valueIn(run),
                tc.getId(),
                tc.getParent().getName());
    }
}
