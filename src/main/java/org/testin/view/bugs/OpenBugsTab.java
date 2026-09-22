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

package org.testin.view.bugs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.BugIssueUrl;
import org.testin.model.FailureDetail;
import org.testin.model.OpenBug;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.ui.FontSync;
import org.testin.util.Bundle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;

public class OpenBugsTab {
    private static final int GAP = 12;

    // UC-VIEW-PANEL-008, Rule-VIEW-PANEL-064
    private static @NotNull JBPanel<?> rows(final @NotNull List<OpenBug> bugs) {
        final @NotNull JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(GAP, 16, 0, 16));

        for (final OpenBug bug : bugs) {
            panel.add(left(heading(bug)));
            if (FailureDetail.isTriaged(bug.item())) panel.add(left(severity(bug)));
            bug.item().bugIssue().ifPresent(url -> panel.add(left(issue(url))));

            if (!bug.item().getActualResult().isBlank()) panel.add(left(note(bug.item().getActualResult())));

            panel.add(Box.createVerticalStrut(JBUI.scale(GAP)));
        }

        return panel;
    }

    private static @NotNull JBLabel heading(final @NotNull OpenBug bug) {
        final @NotNull JBLabel label = new JBLabel(bug.runName());
        label.setFont(JBFont.label().asBold().deriveFont(FontSync.getBaseFontSize()));

        return label;
    }

    private static @NotNull JBLabel severity(final @NotNull OpenBug bug) {
        final @NotNull JBLabel label = new JBLabel(
                bug.item().getBugSeverity().getLabel() + " / " + bug.item().getBugPriority().getLabel());

        label.setForeground(bug.item().getBugSeverity().getColor());
        label.setFont(JBFont.label().deriveFont(FontSync.getBaseFontSize()));

        return label;
    }

    private static @NotNull ActionLink issue(final @NotNull String url) {
        final @NotNull ActionListener open = event -> BugIssueUrl.open(url);
        final @NotNull ActionLink link = new ActionLink(BugIssueUrl.reference(url), open);
        link.setFont(JBFont.label().deriveFont(FontSync.getBaseFontSize()));

        return link;
    }

    private static @NotNull JBLabel note(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setForeground(JBColor.GRAY);
        label.setFont(JBFont.label().deriveFont(FontSync.getBaseFontSize()));

        return label;
    }

    private static @NotNull JComponent left(final @NotNull JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        return row;
    }

    // UC-VIEW-PANEL-008, Rule-VIEW-PANEL-038, Rule-VIEW-PANEL-064
    public void load(final @NotNull Project p, final @NotNull JBPanel<?> bugTab, final @NotNull Optional<TestCaseDto> shown) {
        bugTab.removeAll();
        bugTab.setLayout(new BorderLayout());

        bugTab.add(contents(p, shown), BorderLayout.NORTH);

        bugTab.revalidate();
        bugTab.repaint();
    }

    private @NotNull JComponent contents(final @NotNull Project p, final @NotNull Optional<TestCaseDto> shown) {
        if (shown.isEmpty()) return note(Bundle.message("view.bugs.no.selection"));

        final @NotNull List<OpenBug> bugs = OpenBug.of(
                Services.getInstance(p, ProjectIndexer.class).getAllTestRuns(), shown.orElseThrow().getId());

        return bugs.isEmpty() ? note(Bundle.message("view.bugs.none")) : rows(bugs);
    }
}
