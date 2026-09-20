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

package org.testin.report.generators;

import org.testin.testrun.RunEditorAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunExecution;
import org.testin.model.TestRunSummary;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.DetailRow;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import org.testin.model.markers.TestRunMarker;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportOverview {
    private static final @NotNull String NOT_RECORDED = Bundle.message("report.overview.not.recorded");

    // Rule-REPORT-002
    public static @NotNull List<DetailRow> rowsFor(final @NotNull String projectName, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull TestRunSummary summary) {
        final @NotNull TestRunMarker marker = trDir.getMarker();
        final @NotNull List<DetailRow> rows = new ArrayList<>();

        rows.add(new DetailRow(Bundle.message("report.overview.project"), projectName));
        rows.add(new DetailRow(Bundle.message("node.tr"), trDir.getName()));

        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (field == TestRunConfiguration.COMMIT_ID) {
                rows.add(new DetailRow(field.getDisplayName(),
                        field.valueIn(marker).isEmpty() ? NOT_RECORDED : field.valueIn(marker)));
                continue;
            }

            if (field == TestRunConfiguration.COMPONENT) continue;

            if (field == TestRunConfiguration.PLATFORM) {
                final @NotNull String platform = TestRunConfiguration.PLATFORM.valueIn(marker);
                final @NotNull String component = TestRunConfiguration.COMPONENT.valueIn(marker);

                add(rows, ReportText.joined(", ",
                                platform.isEmpty() ? "" : TestRunConfiguration.PLATFORM.getDisplayName(),
                                component.isEmpty() ? "" : TestRunConfiguration.COMPONENT.getDisplayName()),
                        ReportText.joined(", ", platform, component));
                continue;
            }

            add(rows, field.getDisplayName(), field.valueIn(marker));
        }

        add(rows, RunEditorAttributes.EXECUTED_BY.getName(), summary.executedBy());
        TestRunExecution.rowsOf(marker).forEach(row -> add(rows, row.caption(), row.value()));
        add(rows, RunEditorAttributes.RUN_STATUS.getName(), marker.getStatus().getLabel());

        return List.copyOf(rows);
    }

    private static void add(final @NotNull List<DetailRow> rows, final @NotNull String caption, final @NotNull String value) {
        if (value.isEmpty()) return;

        rows.add(new DetailRow(caption, value));
    }
}
