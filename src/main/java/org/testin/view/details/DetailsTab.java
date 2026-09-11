package org.testin.view.details;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.testrun.RunEditorAttributes;
import org.testin.codegen.ExecutionPosition;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.ui.FontSync;
import org.testin.view.details.components.ActionIcons;
import org.testin.view.details.components.AttributeRow;
import org.testin.view.details.components.BadgeRow;
import org.testin.view.details.components.BaseDetails;
import org.testin.view.details.components.Id;
import org.testin.view.details.components.NavigationBar;
import org.testin.view.details.components.RunAttributeRow;
import org.testin.view.details.components.StacktraceRow;
import org.testin.view.details.components.Steps;
import org.testin.view.details.components.Title;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.List;
import java.util.stream.Stream;

/**
 * What a test case is, drawn: its title, its rows, the run's record of it, and
 * its badges.
 * <p>
 * Only that. Editing the case it is showing is {@link EditShownCase}, which took
 * 85 of this class's 322 lines with it when #302 split them - the key that opens
 * the update menu, the write that follows, and the question of where a case
 * shown from a search result would even be written to. Drawing a case and
 * opening the editor for it are two jobs, and this one is the first.
 */
public class DetailsTab {


    final int SCROLL_UNIT_INCREMENT = 16;
    final @NotNull String PLACEHOLDER_TEXT = Bundle.message("details.placeholder");
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

    // UC-VIEW-PANEL-004
    public void load(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab, final @NotNull Optional<TestCaseDto> dto, final @NotNull List<String> currentPath) {
        detailsTab.removeAll();
        detailsTab.setLayout(new BorderLayout());
        detailsTab.setBorder(BorderFactory.createEmptyBorder());

        dto.ifPresentOrElse(
                testCase -> renderCase(p, detailsTab, testCase, runItemFor(p, testCase, currentPath), currentPath),
                () -> renderPlaceholder(detailsTab));

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void renderCase(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab, final @NotNull TestCaseDto dto, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        final @NotNull JBPanel<?> contentPanel = new JBPanel<>(new GridBagLayout());
        contentPanel.setOpaque(false);

        renderStoneLayout(p, contentPanel, dto, runItem, currentPath);

        final @NotNull JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);

        FontSync.attachWheelZoom(p, contentPanel);

        detailsTab.add(scrollPane, BorderLayout.CENTER);

        EditShownCase.bindTo(p, detailsTab);
    }

    /**
     * The run's own record of this case, and empty when the case is not being
     * viewed under one.
     * <p>
     * Found from the path rather than passed in, because the path is the only
     * thing the two doors into this panel agree on - the run editor's context
     * menu and its grid double-click both hand over the node the selection came
     * from, and the test editor hands over a test set, which resolves to no run
     * and is exactly the case that must show no run rows.
     * <p>
     * The row comes from the indexer, which is the same object the run editor
     * loaded and writes verdicts into, so the panel shows what the run holds
     * now rather than a copy of what it held when it was opened.
     */
    private static @NotNull Optional<TestRunItems> runItemFor(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath) {
        if (currentPath.isEmpty()) return Optional.empty();

        return Services.getInstance(p, ProjectIndexer.class)
                .findTestRun(Services.getInstance(p, TestinRoot.class).resolve(currentPath))
                .flatMap(run -> run.getResults().stream().filter(item -> item.getId().equals(dto.getId())).findFirst());
    }

    private void renderPlaceholder(final @NotNull JBPanel<?> panel) {
        panel.setLayout(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(25, 16, 0, 0));
        final @NotNull JBLabel placeholder = new JBLabel(PLACEHOLDER_TEXT);
        placeholder.setForeground(JBColor.GRAY);
        placeholder.setFont(JBFont.label().deriveFont(FontSync.getBaseFontSize()));
        panel.add(placeholder, BorderLayout.NORTH);
    }

    private void renderStoneLayout(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull TestCaseDto dto, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(INSETS_DEFAULT);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = WEIGHT_X;

        final int row = setupFixedRows(p, panel, gbc, dto, runItem, currentPath);
        addVerticalSpacer(panel, row);
    }

    /**
     * Rows shown in the details panel, in display order. Rows with custom rendering
     * are dedicated components; plain label/value rows are table-driven.
     */
    private @NotNull List<BaseDetails> detailRows(final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        return Stream.of(
                Stream.of(
                        new NavigationBar(currentPath),
                        new Id(),
                        new Title(),
                        new ActionIcons(),
                        new BadgeRow()),
                runItem.stream().flatMap(DetailsTab::runRows),
                caseRows()
        ).flatMap(rows -> rows).toList();
    }

    /**
     * What one execution of this case recorded, in the order a tester asks it:
     * the verdict, how long it took, what actually happened, why, and how bad
     * the bug is.
     * <p>
     * Above the case's own attributes, because a tester who opened this panel
     * during a run came for these. Below the badges, because the case still has
     * to be identifiable first.
     * <p>
     * Present only when the case is being viewed under a run - opened from a run
     * editor rather than from the test editor or a search result. A case nobody
     * has run has no verdict and no duration, and six rows saying so with a dash
     * are six lines read on every case to learn nothing.
     */
    private static @NotNull Stream<BaseDetails> runRows(final @NotNull TestRunItems item) {
        return Stream.of(
                new RunAttributeRow(RunEditorAttributes.RUN_STATUS, item),
                new RunAttributeRow(RunEditorAttributes.DURATION, item),
                new RunAttributeRow(RunEditorAttributes.ACTUAL_RESULT, item),
                new StacktraceRow(item),
                new RunAttributeRow(RunEditorAttributes.BUG_SEVERITY, item),
                new RunAttributeRow(RunEditorAttributes.BUG_PRIORITY, item));
    }

    /**
     * The case itself: the same rows wherever it is shown.
     */
    private static @NotNull Stream<BaseDetails> caseRows() {
        return Stream.of(
                new AttributeRow(TestEditorAttributes.EXPECTED_RESULT.getName(), (p, dto) -> TestEditorAttributes.EXPECTED_RESULT.displayValue(dto)),
                new Steps(),
                new AttributeRow(TestEditorAttributes.PRE_CONDITIONS.getName(), (p, dto) -> TestEditorAttributes.PRE_CONDITIONS.displayValue(dto)),
                // Verbatim, and not through Display: test data is credentials, a
                // query, a payload - values that are used, not read, so a
                // character this panel decides to drop is a value that no longer
                // works. The line breaks are the tester's own now that the field
                // is multi-line, and the row renders them.
                new AttributeRow(TestEditorAttributes.TEST_DATA.getName(), (p, dto) -> TestEditorAttributes.TEST_DATA.displayValue(dto)),
                // No FQCN row. The fully qualified class and method name is how
                // the plugin finds the generated code to navigate to and run -
                // it is machinery, not something a tester reads while executing.
                // It stays available as a toolbar attribute for anyone who wants
                // it on the card or in the grid; it is only off the always-on
                // panel.
                new AttributeRow(TestEditorAttributes.REFERENCE.getName(), (p, dto) -> TestEditorAttributes.REFERENCE.displayValue(dto)),
                new AttributeRow(TestEditorAttributes.MODULE.getName(), (p, dto) -> TestEditorAttributes.MODULE.displayValue(dto)),
                // Where the case sits in its set, which is the number the card
                // draws before the description and the number a generated
                // method carries as its priority. Read from the set rather than
                // the case: a position is what the set says, not something the
                // case stores.
                new AttributeRow(TestEditorAttributes.ORDER.getName(), (p, dto) -> String.valueOf(ExecutionPosition.of(p, dto))),
                // Two rows for two facts, not four. Who and when read as one
                // thing, and four captions to say two of them filled a quarter
                // of the panel with words nobody needed twice (#23).
                new AttributeRow(Bundle.message("details.created"), (p, dto) -> Display.whoAndWhen(dto.getCreatedBy(), dto.getCreatedAt())),
                new AttributeRow(Bundle.message("details.updated"), (p, dto) -> Display.whoAndWhen(dto.getUpdatedBy(), dto.getUpdatedAt()))
        );
    }

    private int setupFixedRows(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        int row = 0;
        for (final BaseDetails component : detailRows(runItem, currentPath)) {
            row = component.render(p, panel, (GridBagConstraints) gbc.clone(), dto, row);
        }
        return row;
    }

    private void addVerticalSpacer(final @NotNull JBPanel<?> panel, final int lastRow) {
        final @NotNull GridBagConstraints spacerGbc = new GridBagConstraints();
        spacerGbc.gridy = lastRow;
        spacerGbc.weighty = SPACER_WEIGHT_Y;
        panel.add(Box.createVerticalGlue(), spacerGbc);
    }



}
