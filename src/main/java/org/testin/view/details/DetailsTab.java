package org.testin.view.details;

import org.testin.notifications.Done;
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
import org.testin.indexer.ProjectIndexer;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testcase.create.TestCaseUpdateMenuDialog;
import org.testin.util.Display;
import org.testin.util.FontSync;
import org.testin.util.Shortcuts;
import org.testin.view.ViewToolWindowFactory;
import org.testin.view.details.components.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.stream.Stream;

public class DetailsTab {

    private static final @NotNull String SHORTCUT_REGISTERED_KEY = "DetailsTab.f2.registered";

    final int SCROLL_UNIT_INCREMENT = 16;
    final @NotNull String PLACEHOLDER_TEXT = "Select a test case to view details";
    final int INSETS_DEFAULT = 5;
    final double WEIGHT_X = 1.0;
    final double SPACER_WEIGHT_Y = 1.0;

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

        registerEditShortcutOnce(p, detailsTab);
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
                Stream.<BaseDetails>of(
                        new NavigationBar(currentPath),
                        new Id(),
                        new Title(),
                        new ActionIcons(),
                        new Badges()),
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
                new AttributeRow(TestEditorAttributes.EXPECTED_RESULT, (p, dto) -> Display.format(dto.getExpectedResult())),
                new Steps(),
                new AttributeRow(TestEditorAttributes.PRE_CONDITIONS, (p, dto) -> Display.format(dto.getPreConditions())),
                new AttributeRow(TestEditorAttributes.TEST_DATA, (p, dto) -> Display.entriesOnLines(dto.getTestData())),
                // No FQCN row. The fully qualified class and method name is how
                // the plugin finds the generated code to navigate to and run -
                // it is machinery, not something a tester reads while executing.
                // It stays available as a toolbar attribute for anyone who wants
                // it on the card or in the grid; it is only off the always-on
                // panel.
                new AttributeRow(TestEditorAttributes.REFERENCE, (p, dto) -> Display.format(dto.getReference())),
                new AttributeRow(TestEditorAttributes.MODULE, (p, dto) -> Display.format(dto.getModule())),
                new AttributeRow(TestEditorAttributes.CREATE_BY, (p, dto) -> dto.getCreatedBy()),
                new AttributeRow(TestEditorAttributes.UPDATE_BY, (p, dto) -> dto.getUpdatedBy()),
                new AttributeRow(TestEditorAttributes.CREATE_AT, (p, dto) -> Display.formatDate(dto.getCreatedAt())),
                new AttributeRow(TestEditorAttributes.UPDATE_AT, (p, dto) -> Display.formatDate(dto.getUpdatedAt()))
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

    private void registerEditShortcutOnce(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab) {
        if (Boolean.TRUE.equals(detailsTab.getClientProperty(SHORTCUT_REGISTERED_KEY))) {
            return;
        }
        detailsTab.putClientProperty(SHORTCUT_REGISTERED_KEY, Boolean.TRUE);

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                ViewToolWindowFactory.panel().ifPresent(viewPanel -> viewPanel.getCurrentTestCase()
                        .ifPresent(currentDto -> openUpdateMenu(p, currentDto, viewPanel.getPage().getCurrentPath())));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
                return ActionUpdateThread.BGT;
            }
        }.registerCustomShortcutSet(Shortcuts.UpdateItem.getCustomShortcut(), detailsTab);
    }

    private void openUpdateMenu(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath) {
        final @NotNull List<TestCaseDto> items = List.of(dto);

        new TestCaseUpdateMenuDialog(p, items, (tcs, gt) -> {
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            resolveEditPath(p, dto, currentPath).ifPresent(editPath ->
                    tcs.forEach(tc -> indexer.putTestCase(editPath, tc)));

            Services.getInstance(p, Notifier.class).softShow(p, Done.UPDATED);

            ApplicationManager.getApplication().invokeLater(() -> TestCaseUpdateMenuDialog.applyAftermath(p, tcs, gt));
        }).show();
    }

    /**
     * Where an update writes: the case's own parent when it has one, otherwise
     * the test set the navigation path names. Empty when neither says - a case
     * shown from a search result, with no path and no parent read yet.
     */
    private @NotNull Optional<Path> resolveEditPath(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath) {
        final @NotNull DirectoryDto parent = dto.getParent();
        if (!parent.getPath().toString().isEmpty()) {
            return Optional.of(parent.getPath());
        }

        if (currentPath.isEmpty()) return Optional.empty();

        final @NotNull Path resolved = Services.getInstance(p, TestinRoot.class).resolve(currentPath);

        return Optional.of(Services.getInstance(p, ProjectIndexer.class).getTestSetByPath(resolved).getPath());
    }
}
