package org.testin.viewPanel.details;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.FontSyncUtil;
import org.testin.viewPanel.details.components.*;
import org.testin.viewPanel.details.components.Module;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class DetailsTab {

    final int SCROLL_UNIT_INCREMENT = 16;
    final String PLACEHOLDER_TEXT = "Select a test case to view details";
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

    public void load(final @NotNull Project project, @NotNull final JBPanel<?> detailsTab, @Nullable final TestCaseDto dto, @Nullable final Path currentPath) {
        detailsTab.removeAll();
        detailsTab.setLayout(new BorderLayout());
        detailsTab.setBorder(BorderFactory.createEmptyBorder());

        if (dto == null) {
            renderPlaceholder(detailsTab);
        } else {
            final JBPanel<?> contentPanel = new JBPanel<>(new GridBagLayout());
            contentPanel.setOpaque(false);

            renderStoneLayout(project, contentPanel, dto, currentPath);

            final JBScrollPane scrollPane = new JBScrollPane(contentPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);

            detailsTab.add(scrollPane, BorderLayout.CENTER);
        }

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void renderPlaceholder(@NotNull final JBPanel<?> panel) {
        panel.setLayout(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(25, 16, 0, 0));
        final JLabel placeholder = new JLabel(PLACEHOLDER_TEXT);
        placeholder.setForeground(JBColor.GRAY);
        placeholder.setFont(JBFont.label().deriveFont(FontSyncUtil.getBaseFontSize() + 5.0f));
        panel.add(placeholder, BorderLayout.NORTH);
    }

    private void renderStoneLayout(final @NotNull Project project, final JBPanel<?> panel, final TestCaseDto dto, final Path currentPath) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(INSETS_DEFAULT);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = WEIGHT_X;

        final int row = setupFixedRows(project, panel, gbc, dto, currentPath);
        addVerticalSpacer(panel, row);
    }

    private int setupFixedRows(final @NotNull Project project, final JBPanel<?> panel, final GridBagConstraints gbc, final TestCaseDto dto, final Path currentPath) {
        int row = 0;

        row = new NavigationBar(project, currentPath).render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Id().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Title().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new ActionIcons(project).render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Badges().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new ExpectedResult().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Steps().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new PreConditions().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new TestData().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        //row = new Fqcn().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row); // todo, commented-001
        row = new Reference().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Module().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new CreatedBy().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new UpdatedBy().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new CreatedAt().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new UpdatedAt().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);

        return row;
    }

    private void addVerticalSpacer(final JBPanel<?> panel, final int lastRow) {
        final GridBagConstraints spacerGbc = new GridBagConstraints();
        spacerGbc.gridy = lastRow;
        spacerGbc.weighty = SPACER_WEIGHT_Y;
        panel.add(Box.createVerticalGlue(), spacerGbc);
    }
}