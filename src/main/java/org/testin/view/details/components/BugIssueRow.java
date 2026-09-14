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
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
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
import org.testin.testrun.RunEditorAttributes;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;

/**
 * The GitHub issue a failure was reported as, and the link that reports it
 * (#28).
 * <p>
 * Its own row type rather than a {@link RunAttributeRow}: the value is two
 * links, and one of them is disabled for reasons that are not on the run item -
 * a report already on its way, another one open, a run signed off.
 */
@AllArgsConstructor
public final class BugIssueRow extends BaseDetails {

    private static final int LINK_GAP = 16;

    private final @NotNull TestRunItems item;
    private final @NotNull List<String> currentPath;

    /**
     * UC-VIEW-PANEL-005, Rule-VIEW-PANEL-031.
     * <p>
     * Drawn for a failed run item, which can be reported, and for any run item
     * that has been - a link, once there, stays until a pass clears it. Report
     * Bug asks why it is off every time the row is drawn, because the panel
     * rebuilds itself on every refresh and a disabled link held anywhere else
     * would be drawn enabled again.
     */
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull String bugIssueUrl = item.getBugIssueUrl();
        if (item.getStatus() != TestStatus.FAILED && bugIssueUrl.isBlank()) return currentRow;

        final @NotNull TestRunDirectoryDto runDirectory = Services.getInstance(p, ProjectIndexer.class)
                .getTestRunDirByPath(Services.getInstance(p, TestinRoot.class).resolve(currentPath));
        final @NotNull Optional<String> off = Services.getInstance(p, BugReports.class)
                .whyReportBugIsOff(new BugReports.RunItem(runDirectory.getPath(), item.getId()), bugIssueUrl, runDirectory.isStillOpen());

        final @NotNull JBPanel<?> links = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        links.setOpaque(false);

        if (!bugIssueUrl.isBlank()) {
            final @NotNull ActionLink issue = link(BugIssueUrl.reference(bugIssueUrl), event -> BugIssueUrl.open(bugIssueUrl));
            issue.setBorder(JBUI.Borders.emptyRight(LINK_GAP));
            links.add(issue);
        }

        final @NotNull ActionLink report = link(Bundle.message("bug.dialog.title"),
                event -> ReportBug.start(p, runDirectory, item.getId(), dto, () -> redraw(p, dto, runDirectory)));
        report.setEnabled(off.isEmpty());
        report.setToolTipText(off.orElse(""));
        links.add(report);

        return addRow(panel, gbc, RunEditorAttributes.BUG_ISSUE.getName(), links, currentRow);
    }

    /**
     * Typed rather than inline: ActionLink also takes a Kotlin function of the
     * same shape, and an untyped lambda matches both.
     */
    private @NotNull ActionLink link(final @NotNull String text, final @NotNull ActionListener onClick) {
        final @NotNull ActionLink link = new ActionLink(text, onClick);
        link.setFont(JBFont.label().deriveFont(getValueFontSize()));
        return link;
    }

    /**
     * Every surface showing the run item: this panel, and the run editor whose
     * Bug Issue column changes when the link is stored.
     */
    private static void redraw(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull TestRunDirectoryDto runDirectory) {
        ViewToolWindowFactory.refreshIfShowing(p, List.of(dto));
        Services.getInstance(p, TestinEditors.class).editorFor(p, runDirectory)
                .filter(RunEditor.class::isInstance)
                .map(RunEditor.class::cast)
                .ifPresent(RunEditor::refreshAfterStatusChange);
    }
}
