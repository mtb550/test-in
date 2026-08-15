package org.testin.view.details;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.Config;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testcase.create.TestCaseUpdateMenuDialog;
import org.testin.util.FontSync;
import org.testin.util.Shortcuts;
import org.testin.util.Tools;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;
import org.testin.view.details.components.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DetailsTab {

    private static final String SHORTCUT_REGISTERED_KEY = "DetailsTab.f2.registered";

    final int SCROLL_UNIT_INCREMENT = 16;
    final @NotNull String PLACEHOLDER_TEXT = "Select a test case to view details";
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

    public void load(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab, final @Nullable TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
        detailsTab.removeAll();
        detailsTab.setLayout(new BorderLayout());
        detailsTab.setBorder(BorderFactory.createEmptyBorder());

        if (dto == null) {
            renderPlaceholder(detailsTab);
        } else {
            final JBPanel<?> contentPanel = new JBPanel<>(new GridBagLayout());
            contentPanel.setOpaque(false);

            renderStoneLayout(p, contentPanel, dto, currentPath);

            final JBScrollPane scrollPane = new JBScrollPane(contentPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);

            FontSync.attachWheelZoom(p, contentPanel);

            detailsTab.add(scrollPane, BorderLayout.CENTER);

            registerEditShortcutOnce(p, detailsTab);
        }

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void renderPlaceholder(final @NotNull JBPanel<?> panel) {
        panel.setLayout(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(25, 16, 0, 0));
        final JBLabel placeholder = new JBLabel(PLACEHOLDER_TEXT);
        placeholder.setForeground(JBColor.GRAY);
        placeholder.setFont(JBFont.label().deriveFont(FontSync.getBaseFontSize()));
        panel.add(placeholder, BorderLayout.NORTH);
    }

    private void renderStoneLayout(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(INSETS_DEFAULT);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = WEIGHT_X;

        final int row = setupFixedRows(p, panel, gbc, dto, currentPath);
        addVerticalSpacer(panel, row);
    }

    /**
     * Rows shown in the details panel, in display order. Rows with custom rendering
     * are dedicated components; plain label/value rows are table-driven.
     */
    private @NotNull List<BaseDetails> detailRows(final @Nullable ArrayList<String> currentPath) {
        return List.of(
                new NavigationBar(currentPath),
                new Id(),
                new Title(),
                new ActionIcons(),
                new Badges(),
                new AttributeRow(TestEditorAttributes.EXPECTED_RESULT, (p, dto) -> Tools.format(dto.getExpectedResult())),
                new Steps(),
                new AttributeRow(TestEditorAttributes.PRE_CONDITIONS, (p, dto) -> Tools.format(dto.getPreConditions())),
                new AttributeRow(TestEditorAttributes.TEST_DATA, (p, dto) -> Tools.format(dto.getTestData())),
                new Fqcn(),
                new AttributeRow(TestEditorAttributes.REFERENCE, (p, dto) -> Tools.format(dto.getReference())),
                new AttributeRow(TestEditorAttributes.MODULE, (p, dto) -> Tools.format(dto.getModule())),
                new AttributeRow(TestEditorAttributes.CREATE_BY, (p, dto) -> dto.getCreatedBy()),
                new AttributeRow(TestEditorAttributes.UPDATE_BY, (p, dto) -> dto.getUpdatedBy()),
                new AttributeRow(TestEditorAttributes.CREATE_AT, (p, dto) -> dto.getCreatedAt().format(Config.getDateFormatterPattern())),
                new AttributeRow(TestEditorAttributes.UPDATE_AT, (p, dto) -> dto.getUpdatedAt().format(Config.getDateFormatterPattern()))
        );
    }

    private int setupFixedRows(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
        int row = 0;
        for (final BaseDetails component : detailRows(currentPath)) {
            row = component.render(p, panel, (GridBagConstraints) gbc.clone(), dto, row);
        }
        return row;
    }

    private void addVerticalSpacer(final @NotNull JBPanel<?> panel, final int lastRow) {
        final GridBagConstraints spacerGbc = new GridBagConstraints();
        spacerGbc.gridy = lastRow;
        spacerGbc.weighty = SPACER_WEIGHT_Y;
        panel.add(Box.createVerticalGlue(), spacerGbc);
    }

    private void registerEditShortcutOnce(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab) {
        if (Boolean.TRUE.equals(detailsTab.getClientProperty(SHORTCUT_REGISTERED_KEY))) {
            return;
        }
        detailsTab.putClientProperty(SHORTCUT_REGISTERED_KEY, Boolean.TRUE);

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                final ViewPanel viewPanel = ViewToolWindowFactory.getViewPanel();
                if (viewPanel == null) return;

                final TestCaseDto currentDto = viewPanel.getCurrentTestCaseDto();
                if (currentDto == null) return;

                final ArrayList<String> path = viewPanel.getPage().getCurrentPath();
                openUpdateMenu(p, currentDto, path);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
                return ActionUpdateThread.BGT;
            }
        }.registerCustomShortcutSet(Shortcuts.UpdateItem.getCustomShortcut(), detailsTab);
    }

    private void openUpdateMenu(final @NotNull Project p, final @NotNull TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
        final List<TestCaseDto> items = List.of(dto);

        new TestCaseUpdateMenuDialog(p, items, (tcs, gt) -> {
            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final Path editPath = resolveEditPath(p, dto, currentPath);

            if (editPath != null) {
                for (final TestCaseDto tc : tcs) {
                    indexer.putTestCase(editPath, tc);
                }
            }

            Services.getInstance(p, Notifier.class).softShow(p, "Updated");

            ApplicationManager.getApplication().invokeLater(() -> TestCaseUpdateMenuDialog.applyAftermath(p, tcs, gt));
        }).show();
    }

    @Nullable
    private Path resolveEditPath(final @NotNull Project p, final @NotNull TestCaseDto dto, final @Nullable ArrayList<String> currentPath) {
        final DirectoryDto parent = dto.getParent();
        if (!parent.getPath().toString().isEmpty()) {
            return parent.getPath();
        }

        if (currentPath != null && !currentPath.isEmpty()) {
            Path root = Services.getInstance(p, TestinRoot.class).getPath();
            if (root.toString().isEmpty()) {
                root = Path.of(p.getBasePath() != null ? p.getBasePath() : "");
            }

            Path resolved = root.isAbsolute() ? root : Path.of(p.getBasePath() != null ? p.getBasePath() : "").resolve(root);
            for (final String segment : currentPath) {
                resolved = resolved.resolve(segment);
            }

            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final TestSetDirectoryDto ts = indexer.getTestSetByPath(resolved);
            if (ts != null)
                return ts.getPath();
        }

        return null;
    }
}
