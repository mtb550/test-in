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

package org.testin.view.details;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.editor.ShownFields;
import org.testin.editor.WheelForwarding;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestCaseEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.ui.FontSync;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Fonts;
import org.testin.view.details.components.AbstractDetails;
import org.testin.view.details.components.AttributeRow;
import org.testin.view.details.components.BadgesAndActions;
import org.testin.view.details.components.Band;
import org.testin.view.details.components.Breadcrumb;
import org.testin.view.details.components.RunItemAttributeRow;
import org.testin.view.details.components.RunItemSummary;
import org.testin.view.details.components.StacktraceLine;
import org.testin.view.details.components.Steps;
import org.testin.view.details.components.Title;

import javax.swing.BorderFactory;
import javax.swing.Box;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class DetailsTab {
    static final @NotNull String TEST_CASE_OPEN = "testin.viewPanel.testCaseOpen";
    final int SCROLL_UNIT_INCREMENT = 16;
    final @NotNull String PLACEHOLDER_TEXT = Bundle.message("details.placeholder");
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-061, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-086, Rule-VIEW-PANEL-090
    private static @NotNull Band testRunBand(final @NotNull TestRunItems runItem, final @NotNull List<String> currentPath, final @NotNull Set<TestRunEditorAttributes> shown) {
        final @NotNull List<AbstractDetails> rows = new ArrayList<>(List.of(new RunItemSummary(runItem, currentPath)));
        if (shown.contains(TestRunEditorAttributes.ACTUAL_RESULT)) rows.add(new RunItemAttributeRow(TestRunEditorAttributes.ACTUAL_RESULT, runItem));
        rows.add(new StacktraceLine(runItem, currentPath, shown.contains(TestRunEditorAttributes.STACKTRACE)));
        if (shown.contains(TestRunEditorAttributes.EXECUTED_BY) || shown.contains(TestRunEditorAttributes.EXECUTED_AT)) {
            rows.add(new AttributeRow(TestRunEditorAttributes.EXECUTED_BY.getName(), (_, _) -> Display.whoAndWhen(runItem.getExecutedBy(), runItem.getExecutedAt())));
        }
        return Band.of(Bundle.message("details.band.run"), rows);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-087, Rule-VIEW-PANEL-090
    private static @NotNull List<AbstractDetails> testCaseFields(final @NotNull Predicate<TestCaseEditorAttributes> shows) {
        final @NotNull List<AbstractDetails> rows = new ArrayList<>();
        if (shows.test(TestCaseEditorAttributes.EXPECTED_RESULT)) rows.add(new AttributeRow(TestCaseEditorAttributes.EXPECTED_RESULT.getName(), (_, dto) -> TestCaseEditorAttributes.EXPECTED_RESULT.displayValue(dto)));
        if (shows.test(TestCaseEditorAttributes.STEPS)) rows.add(new Steps());
        if (shows.test(TestCaseEditorAttributes.PRE_CONDITIONS)) rows.add(new AttributeRow(TestCaseEditorAttributes.PRE_CONDITIONS.getName(), (_, dto) -> TestCaseEditorAttributes.PRE_CONDITIONS.displayValue(dto)));
        if (shows.test(TestCaseEditorAttributes.TEST_DATA)) rows.add(new AttributeRow(TestCaseEditorAttributes.TEST_DATA.getName(), (_, dto) -> TestCaseEditorAttributes.TEST_DATA.displayValue(dto)));
        if (shows.test(TestCaseEditorAttributes.REFERENCE)) rows.add(new AttributeRow(TestCaseEditorAttributes.REFERENCE.getName(), (_, dto) -> TestCaseEditorAttributes.REFERENCE.displayValue(dto)));
        if (shows.test(TestCaseEditorAttributes.MODULE)) rows.add(new AttributeRow(TestCaseEditorAttributes.MODULE.getName(), (_, dto) -> TestCaseEditorAttributes.MODULE.displayValue(dto)));
        if (shows.test(TestCaseEditorAttributes.ORDER)) rows.add(new AttributeRow(TestCaseEditorAttributes.ORDER.getName(), (p, dto) -> String.valueOf(ExecutionPosition.of(p, dto))));
        if (shows.test(TestCaseEditorAttributes.CREATED_BY) || shows.test(TestCaseEditorAttributes.CREATED_AT)) {
            rows.add(new AttributeRow(Bundle.message("details.created"), (_, dto) -> Display.whoAndWhen(dto.getCreatedBy(), dto.getCreatedAt())));
        }
        if (shows.test(TestCaseEditorAttributes.UPDATED_BY) || shows.test(TestCaseEditorAttributes.UPDATED_AT)) {
            rows.add(new AttributeRow(Bundle.message("details.updated"), (_, dto) -> Display.whoAndWhen(dto.getUpdatedBy(), dto.getUpdatedAt())));
        }
        return rows;
    }

    // Rule-VIEW-PANEL-090
    static @NotNull Predicate<TestCaseEditorAttributes> shownInTheTestRun(final @NotNull Set<TestRunEditorAttributes> shown) {
        return field -> Arrays.stream(TestRunEditorAttributes.values())
                .filter(offered -> offered.name().equals(field.name()))
                .findFirst()
                .map(shown::contains)
                .orElse(true);
    }

    // UC-VIEW-PANEL-004
    public void load(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab, final @NotNull Optional<TestCaseDto> dto, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        detailsTab.removeAll();
        detailsTab.setLayout(new BorderLayout());
        detailsTab.setBorder(BorderFactory.createEmptyBorder());

        dto.ifPresentOrElse(
                testCase -> renderTestCase(p, detailsTab, testCase, runItem, currentPath),
                () -> renderPlaceholder(detailsTab));

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void renderTestCase(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab, final @NotNull TestCaseDto dto, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        final @NotNull JBPanel<?> contentPanel = new JBPanel<>(new GridBagLayout());
        contentPanel.setOpaque(false);

        renderRows(p, contentPanel, dto, runItem, currentPath);

        final @NotNull JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);

        FontSync.attachWheelZoom(p, contentPanel);

        contentPanel.addMouseWheelListener(WheelForwarding::forwardWheelToScrollPane);

        detailsTab.add(scrollPane, BorderLayout.CENTER);

        EditShownTestCase.bindTo(p, detailsTab);
    }

    private void renderPlaceholder(final @NotNull JBPanel<?> panel) {
        panel.setLayout(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(25, 16, 0, 0));
        final @NotNull JBLabel placeholder = new JBLabel(PLACEHOLDER_TEXT);
        placeholder.setForeground(JBColor.GRAY);
        placeholder.setFont(Fonts.body());
        panel.add(placeholder, BorderLayout.NORTH);
    }

    private void renderRows(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull TestCaseDto dto, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(INSETS_DEFAULT);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = WEIGHT_X;

        final int row = setupFixedRows(p, panel, gbc, dto, runItem, currentPath);
        addVerticalSpacer(panel, row);
    }

    // UC-VIEW-PANEL-004, UC-VIEW-PANEL-005, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-087
    private @NotNull List<AbstractDetails> detailRows(final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        final @NotNull List<AbstractDetails> rows = new ArrayList<>(List.of(new Breadcrumb(currentPath), new Title(), new BadgesAndActions()));

        runItem.ifPresentOrElse(shown -> {
            final @NotNull Set<TestRunEditorAttributes> fields = ShownFields.inTestRuns();
            rows.add(testRunBand(shown, currentPath, fields));
            rows.add(Band.folding(Bundle.message("details.band.test.case"), TEST_CASE_OPEN, testCaseFields(shownInTheTestRun(fields))));
        }, () -> rows.addAll(testCaseFields(ShownFields.inTestSets()::contains)));

        return List.copyOf(rows);
    }

    private int setupFixedRows(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        int row = 0;
        for (final AbstractDetails component : detailRows(runItem, currentPath)) {
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
