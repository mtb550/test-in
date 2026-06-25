package org.testin.viewPanel.details;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.settings.Setting;
import org.testin.ui.testCase.TestCaseUpdateMenu;
import org.testin.util.FontSyncUtil;
import org.testin.util.KeyboardSet;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;
import org.testin.util.services.TestCaseCacheService;
import org.testin.util.services.TestCasePersistService;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;
import org.testin.viewPanel.details.components.*;
import org.testin.viewPanel.details.components.Module;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DetailsTab {

    private static final String SHORTCUT_REGISTERED_KEY = "DetailsTab.f2.registered";

    final int SCROLL_UNIT_INCREMENT = 16;
    final String PLACEHOLDER_TEXT = "Select a test case to view details";
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

    public void load(final @NotNull Project project, final @NotNull JBPanel<?> detailsTab, final @Nullable TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
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

            registerEditShortcutOnce(project, detailsTab, dto, currentPath);
        }

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void renderPlaceholder(final @NotNull JBPanel<?> panel) {
        panel.setLayout(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(25, 16, 0, 0));
        final JLabel placeholder = new JLabel(PLACEHOLDER_TEXT);
        placeholder.setForeground(JBColor.GRAY);
        placeholder.setFont(JBFont.label().deriveFont(FontSyncUtil.getBaseFontSize() + 5.0f));
        panel.add(placeholder, BorderLayout.NORTH);
    }

    private void renderStoneLayout(final @NotNull Project project, final JBPanel<?> panel, final TestCaseDto dto, final ArrayList<String> currentPath) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(INSETS_DEFAULT);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = WEIGHT_X;

        final int row = setupFixedRows(project, panel, gbc, dto, currentPath);
        addVerticalSpacer(panel, row);
    }

    private int setupFixedRows(final @NotNull Project project, final JBPanel<?> panel, final GridBagConstraints gbc, final TestCaseDto dto, final ArrayList<String> currentPath) {
        int row = 0;

        row = new NavigationBar(currentPath).render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Id().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Title().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new ActionIcons().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Badges().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new ExpectedResult().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Steps().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new PreConditions().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new TestData().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
        row = new Fqcn().render(project, panel, (GridBagConstraints) gbc.clone(), dto, row);
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

    private void registerEditShortcutOnce(final @NotNull Project project, final @NotNull JBPanel<?> detailsTab, final @NotNull TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
        if (Boolean.TRUE.equals(detailsTab.getClientProperty(SHORTCUT_REGISTERED_KEY))) {
            return;
        }
        detailsTab.putClientProperty(SHORTCUT_REGISTERED_KEY, Boolean.TRUE);

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                openUpdateMenu(project, dto, currentPath);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        }.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getCustomShortcut(), detailsTab);
    }

    private void openUpdateMenu(final @NotNull Project project, final @NotNull TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
        final List<TestCaseDto> items = List.of(dto);

        new TestCaseUpdateMenu(project, items, (updatedItems, codeGenerator) -> {
            // todo, call indexer instead then it will update cache and file
            Services.getInstance(project, TestCaseCacheService.class).addNewItems(updatedItems);

            final Path editPath = resolveEditPath(project, dto, currentPath);
            if (editPath != null) {
                Services.getInstance(project, TestCasePersistService.class).persist(editPath, updatedItems);
            }

            Services.getInstance(project, Notifier.class).softShow(project, "Updated..");

            ApplicationManager.getApplication().invokeLater(() -> {
                final ViewPanel detailsPanel = ViewToolWindowFactory.getViewPanel();
                if (detailsPanel != null && detailsPanel.getCurrentTestCaseDto() != null) {
                    boolean isCurrentAffected = updatedItems.stream()
                            .anyMatch(item -> item.getId().equals(detailsPanel.getCurrentTestCaseDto().getId()));
                    if (isCurrentAffected) {
                        detailsPanel.refreshCurrentView();
                    }
                }

                if (codeGenerator != null && codeGenerator.isSelected()) {
                    Log.trace("[DetailsTab] Code generator selected: " + codeGenerator.getGeneratorType());
                }
            });
        }).show();
    }

    @Nullable
    private Path resolveEditPath(final @NotNull Project project, final @NotNull TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
        final DirectoryDto parent = dto.getParent();
        if (!parent.getPath().toString().isEmpty()) {
            return parent.getPath();
        }

        if (currentPath != null && !currentPath.isEmpty()) {
            Path root = Services.getInstance(project, Setting.class).getTestinPath();
            if (root.toString().isEmpty()) {
                root = Path.of(project.getBasePath() != null ? project.getBasePath() : "");
            }

            Path resolved = root.isAbsolute() ? root : Path.of(project.getBasePath() != null ? project.getBasePath() : "").resolve(root);
            for (final String segment : currentPath) {
                resolved = resolved.resolve(segment);
            }

            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
            final TestSetDirectoryDto ts = indexer.getTestSetByPath(resolved);
            if (ts != null) {
                return ts.getPath();
            }
        }

        return null;
    }
}
