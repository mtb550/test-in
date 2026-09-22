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
import com.intellij.util.ui.JBFont;
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
import org.testin.view.details.components.ActionIcons;
import org.testin.view.details.components.AttributeRow;
import org.testin.view.details.components.BadgeRow;
import org.testin.view.details.components.BaseDetails;
import org.testin.view.details.components.BugIssueRow;
import org.testin.view.details.components.Id;
import org.testin.view.details.components.NavigationBar;
import org.testin.view.details.components.RunAttributeRow;
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

    private static @NotNull Stream<BaseDetails> runRows(final @NotNull TestRunItems item, final @NotNull List<String> currentPath) {
        return Stream.of(
                new RunAttributeRow(RunEditorAttributes.RUN_STATUS, item),
                new RunAttributeRow(RunEditorAttributes.DURATION, item),
                new RunAttributeRow(RunEditorAttributes.ACTUAL_RESULT, item),
                new StacktraceRow(item, currentPath),
                new RunAttributeRow(RunEditorAttributes.BUG_SEVERITY, item),
                new RunAttributeRow(RunEditorAttributes.BUG_PRIORITY, item),
                new BugIssueRow(item, currentPath));
    }

    private static @NotNull Stream<BaseDetails> testCaseRows() {
        return Stream.of(
                new AttributeRow(TestEditorAttributes.EXPECTED_RESULT.getName(), (p, dto) -> TestEditorAttributes.EXPECTED_RESULT.displayValue(dto)),
                new Steps(),
                new AttributeRow(TestEditorAttributes.PRE_CONDITIONS.getName(), (p, dto) -> TestEditorAttributes.PRE_CONDITIONS.displayValue(dto)),
                new AttributeRow(TestEditorAttributes.TEST_DATA.getName(), (p, dto) -> TestEditorAttributes.TEST_DATA.displayValue(dto)),
                new AttributeRow(TestEditorAttributes.REFERENCE.getName(), (p, dto) -> TestEditorAttributes.REFERENCE.displayValue(dto)),
                new AttributeRow(TestEditorAttributes.MODULE.getName(), (p, dto) -> TestEditorAttributes.MODULE.displayValue(dto)),
                new AttributeRow(TestEditorAttributes.ORDER.getName(), (p, dto) -> String.valueOf(ExecutionPosition.of(p, dto))),
                new AttributeRow(Bundle.message("details.created"), (p, dto) -> Display.whoAndWhen(dto.getCreatedBy(), dto.getCreatedAt())),
                new AttributeRow(Bundle.message("details.updated"), (p, dto) -> Display.whoAndWhen(dto.getUpdatedBy(), dto.getUpdatedAt()))
        );
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

    private @NotNull List<BaseDetails> detailRows(final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        return Stream.of(
                Stream.of(
                        new NavigationBar(currentPath),
                        new Id(),
                        new Title(),
                        new ActionIcons(),
                        new BadgeRow()),
                runItem.stream().flatMap(item -> runRows(item, currentPath)),
                testCaseRows()
        ).flatMap(rows -> rows).toList();
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
