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

package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.bug.BugReports;
import org.testin.bug.ReportBug;
import org.testin.editor.TestinEditors;
import org.testin.editor.run.RunEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.BugIssueUrl;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.ui.Badges;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.JComponent;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Bug {
    private static final int LINK_GAP = 10;

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-016, Rule-VIEW-PANEL-031, Rule-VIEW-PANEL-086
    public static @NotNull Optional<JComponent> of(final @NotNull Project p, final @NotNull TestRunItems runItem, final @NotNull List<String> currentPath, final @NotNull TestCaseDto dto) {
        final @NotNull Optional<String> bugIssue = runItem.bugIssue();
        if (runItem.shownStatus() != TestStatus.FAILED && bugIssue.isEmpty()) return Optional.empty();

        return Services.getInstance(p, ProjectIndexer.class).find(Services.getInstance(p, TestinRoot.class).resolve(currentPath))
                .filter(TestRunDirectoryDto.class::isInstance)
                .map(TestRunDirectoryDto.class::cast)
                .map(runDirectory -> drawn(p, runItem, dto, bugIssue, runDirectory));
    }

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-016, Rule-VIEW-PANEL-066, Rule-VIEW-PANEL-075, Rule-VIEW-PANEL-086
    private static @NotNull JComponent drawn(final @NotNull Project p, final @NotNull TestRunItems runItem, final @NotNull TestCaseDto dto, final @NotNull Optional<String> bugIssue, final @NotNull TestRunDirectoryDto runDirectory) {
        final @NotNull JBPanel<?> line = AbstractDetails.row(LINK_GAP);

        chip(runItem).ifPresent(line::add);
        bugIssue.ifPresentOrElse(
                url -> line.add(issue(url)),
                () -> line.add(report(p, runItem, dto, runDirectory)));

        return line;
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-075
    private static @NotNull ActionLink issue(final @NotNull String url) {
        final @NotNull ActionLink link = AbstractDetails.link(BugIssueUrl.shortReference(url), _ -> BugIssueUrl.open(url));

        link.setToolTipText(url);

        return link;
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-066, Rule-VIEW-PANEL-075
    private static @NotNull ActionLink report(final @NotNull Project p, final @NotNull TestRunItems runItem, final @NotNull TestCaseDto dto, final @NotNull TestRunDirectoryDto runDirectory) {
        final @NotNull Optional<String> off = Services.getInstance(p, BugReports.class)
                .whyReportBugIsOff(new BugReports.RunItem(runDirectory.getPath(), runItem.getId()), runItem);

        final @NotNull ActionLink report = AbstractDetails.link(Bundle.message("bug.dialog.title"),
                _ -> ReportBug.start(p, runDirectory, runItem.getId(), dto, () -> redraw(p, dto, runDirectory)));

        report.setEnabled(off.isEmpty());
        report.setToolTipText(off.orElse(""));

        return report;
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-086
    private static @NotNull Optional<JComponent> chip(final @NotNull TestRunItems runItem) {
        final @NotNull List<Badges.Badge> bug = new ArrayList<>();
        Badges.addBugBadge(bug, runItem.getBugSeverity().getLabel(), runItem.getBugSeverity().getColor());
        Badges.addBugBadge(bug, runItem.getBugPriority().getLabel(), runItem.getBugSeverity().getColor());

        if (bug.isEmpty()) return Optional.empty();

        final @NotNull JBPanel<?> holder = AbstractDetails.row(0);
        Badges.showBadges(holder, bug);

        return Optional.of(holder);
    }

    private static void redraw(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull TestRunDirectoryDto runDirectory) {
        ViewToolWindowFactory.refreshIfShowing(p, List.of(dto));
        Services.getInstance(p, TestinEditors.class).runEditorFor(p, runDirectory).ifPresent(RunEditor::refreshView);
    }
}
