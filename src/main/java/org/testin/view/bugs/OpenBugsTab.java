package org.testin.view.bugs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.OpenBug;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.ui.FontSync;

import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.Optional;

/**
 * Every bug this test case has, and which cycle found it.
 * <p>
 * It used to say <i>No bugs found for this test case</i> whatever the case had,
 * because it never looked at one - so a case showing Blocker and High on the
 * Details tab was told beside it that it had none. Two tabs describing the same
 * test case have to agree, which is the rule the panel is built on (#229).
 * <p>
 * A bug is not a thing of its own here. It is what a run row records about a
 * failure, so this reads the runs rather than the case: the same case can carry
 * a Blocker in one cycle and nothing in the next, and both are worth seeing.
 */
public class OpenBugsTab {

    private static final int GAP = 12;

    /**
     * UC-VIEW-PANEL-008, Rule-VIEW-PANEL-038, Rule-VIEW-PANEL-064.
     * <p>
     * With no test case shown there is no test case to say anything about, which
     * is why the empty case is asked first and answers differently (#230).
     */
    public void load(final @NotNull Project p, final @NotNull JBPanel<?> bugTab, final @NotNull Optional<TestCaseDto> shown) {
        bugTab.removeAll();
        bugTab.setLayout(new BorderLayout());

        if (shown.isEmpty()) {
            bugTab.add(note("Select a test case to view its bugs"), BorderLayout.NORTH);
            return;
        }

        final @NotNull List<OpenBug> bugs = OpenBug.of(
                Services.getInstance(p, ProjectIndexer.class).getAllTestRuns(), shown.orElseThrow().getId());

        if (bugs.isEmpty()) {
            bugTab.add(note("No bugs recorded for this test case in any test run"), BorderLayout.NORTH);
            return;
        }

        bugTab.add(rows(bugs), BorderLayout.NORTH);
    }

    /**
     * One block per bug: which run found it, how bad it is, and what happened.
     */
    private static @NotNull JBPanel<?> rows(final @NotNull List<OpenBug> bugs) {
        final @NotNull JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(GAP, 16, 0, 16));

        for (final OpenBug bug : bugs) {
            panel.add(left(heading(bug)));
            panel.add(left(severity(bug)));

            if (!bug.item().getActualResult().isBlank()) panel.add(left(note(bug.item().getActualResult())));

            panel.add(Box.createVerticalStrut(JBUI.scale(GAP)));
        }

        return panel;
    }

    /**
     * The run's name, which is the only thing that says which cycle found this.
     */
    private static @NotNull JBLabel heading(final @NotNull OpenBug bug) {
        final @NotNull JBLabel label = new JBLabel(bug.runName());
        label.setFont(JBFont.label().asBold().deriveFont(FontSync.getBaseFontSize()));

        return label;
    }

    /**
     * How bad and how soon, in the severity's own color - the same one the run
     * grid and the reports paint it, because the constant carries it.
     */
    private static @NotNull JBLabel severity(final @NotNull OpenBug bug) {
        final @NotNull JBLabel label = new JBLabel(
                bug.item().getBugSeverity().getLabel() + " / " + bug.item().getBugPriority().getLabel());

        label.setForeground(bug.item().getBugSeverity().getColor());
        label.setFont(JBFont.label().deriveFont(FontSync.getBaseFontSize()));

        return label;
    }

    private static @NotNull JBLabel note(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setForeground(JBColor.GRAY);
        label.setFont(JBFont.label().deriveFont(FontSync.getBaseFontSize()));

        return label;
    }

    /**
     * A box layout stretches its children, so every label is told to sit left
     * rather than in the middle of whatever width the tab has.
     */
    private static @NotNull JBLabel left(final @NotNull JBLabel label) {
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }
}
