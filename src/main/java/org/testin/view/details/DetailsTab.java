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
import org.testin.editor.WheelForwarding;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testrun.RunEditorAttributes;
import org.testin.ui.FontSync;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Fonts;
import org.testin.view.details.components.ActionIcons;
import org.testin.view.details.components.AttributeRow;
import org.testin.view.details.components.BandTitle;
import org.testin.view.details.components.BaseDetails;
import org.testin.view.details.components.BugIssueRow;
import org.testin.view.details.components.Identity;
import org.testin.view.details.components.MoreFields;
import org.testin.view.details.components.NavigationBar;
import org.testin.view.details.components.RunAttributeRow;
import org.testin.view.details.components.RunSummary;
import org.testin.view.details.components.StacktraceRow;
import org.testin.view.details.components.Steps;
import org.testin.view.details.components.Title;

import javax.swing.BorderFactory;
import javax.swing.Box;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class DetailsTab {
    final int SCROLL_UNIT_INCREMENT = 16;
    final @NotNull String PLACEHOLDER_TEXT = Bundle.message("details.placeholder");
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-086
    private static @NotNull List<BaseDetails> runBand(final @NotNull TestRunItems item, final @NotNull List<String> currentPath) {
        return List.of(
                new BandTitle(Bundle.message("details.band.run")),
                new RunSummary(item),
                new RunAttributeRow(RunEditorAttributes.ACTUAL_RESULT, item),
                new StacktraceRow(item, currentPath),
                new BugIssueRow(item, currentPath));
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-085, Rule-VIEW-PANEL-087
    private static @NotNull List<BaseDetails> testCaseBand() {
        return List.of(
                new BandTitle(Bundle.message("details.band.case")),
                new AttributeRow(TestEditorAttributes.EXPECTED_RESULT.getName(), (_, dto) -> TestEditorAttributes.EXPECTED_RESULT.displayValue(dto)),
                new Steps(),
                new AttributeRow(TestEditorAttributes.PRE_CONDITIONS.getName(), (_, dto) -> TestEditorAttributes.PRE_CONDITIONS.displayValue(dto)),
                new AttributeRow(TestEditorAttributes.TEST_DATA.getName(), (_, dto) -> TestEditorAttributes.TEST_DATA.displayValue(dto)),
                new MoreFields(folded()));
    }

    // Rule-VIEW-PANEL-087
    private static @NotNull List<BaseDetails> folded() {
        return List.of(
                new AttributeRow(TestEditorAttributes.REFERENCE.getName(), (_, dto) -> TestEditorAttributes.REFERENCE.displayValue(dto)),
                new AttributeRow(TestEditorAttributes.MODULE.getName(), (_, dto) -> TestEditorAttributes.MODULE.displayValue(dto)),
                new AttributeRow(TestEditorAttributes.ORDER.getName(), (p, dto) -> String.valueOf(ExecutionPosition.of(p, dto))),
                new AttributeRow(Bundle.message("details.created"), (_, dto) -> Display.whoAndWhen(dto.getCreatedBy(), dto.getCreatedAt())),
                new AttributeRow(Bundle.message("details.updated"), (_, dto) -> Display.whoAndWhen(dto.getUpdatedBy(), dto.getUpdatedAt())));
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

        renderStoneLayout(p, contentPanel, dto, runItem, currentPath);

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

    private void renderStoneLayout(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull TestCaseDto dto, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(INSETS_DEFAULT);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = WEIGHT_X;

        final int row = setupFixedRows(p, panel, gbc, dto, runItem, currentPath);
        addVerticalSpacer(panel, row);
    }

    // UC-VIEW-PANEL-004, UC-VIEW-PANEL-005, Rule-VIEW-PANEL-085
    private @NotNull List<BaseDetails> detailRows(final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        return Stream.of(
                List.<BaseDetails>of(new NavigationBar(currentPath), new Title(), new Identity()),
                runItem.map(item -> runBand(item, currentPath)).orElse(List.of()),
                testCaseBand()
        ).flatMap(List::stream).toList();
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
