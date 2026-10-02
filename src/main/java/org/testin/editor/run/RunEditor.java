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

package org.testin.editor.run;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.BaseCard;
import org.testin.editor.EditorFilters;
import org.testin.editor.PageWindow;
import org.testin.editor.TestCaseFilter;
import org.testin.editor.UnifiedVirtualFile;
import org.testin.editor.listeners.RunGridEditListener;
import org.testin.editor.listeners.RunListRenderer;
import org.testin.editor.listeners.StatusBarListener;
import org.testin.editor.toolbar.RunToolbar;
import org.testin.editor.toolbar.Toolbar;
import org.testin.editor.toolbar.components.GenerateReportBtn;
import org.testin.editor.toolbar.components.LightModeBtn;
import org.testin.editor.toolbar.components.ResultAnalysisBtn;
import org.testin.editor.toolbar.components.RunDetailsPopupBtn;
import org.testin.editor.toolbar.components.StartExecutionBtn;
import org.testin.editor.toolbar.components.StopExecutionBtn;
import org.testin.indexer.TestRuns;
import org.testin.lightmode.LightMode;
import org.testin.logger.Logger;
import org.testin.model.Modules;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestRunSummary;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Done;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testrun.ResultAnalysisDialog;
import org.testin.testrun.RunEditorAttributes;
import org.testin.ui.SideScroll;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.FailureText;

import java.awt.BorderLayout;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RunEditor extends AbstractTestinEditor<RunEditorAttributes, TestRunDirectoryDto> implements Toolbar {
    private final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);

    private final @NotNull LightMode lightMode = Services.getInstance(p, LightMode.class);

    private final @NotNull Map<UUID, TestRunItems> resultsMap = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull RunToolbar toolBar;

    @Getter
    private final @NotNull RunWalk walk = new RunWalk(this, p);

    private final @NotNull AtomicInteger loadGeneration = new AtomicInteger();

    private volatile @NotNull Optional<TestRunDto> run = Optional.empty();

    private boolean loaded;

    private boolean startWhenLoaded;

    public RunEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        super(p, vf.getTestRun());

        TestCaseExecutionSubscriber.onReported(p, projectDisposable, walk::executionReported);

        this.toolBar = new RunToolbar(p, this);
        buildOpeningPanel();
        loadDataAsync();
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    @Override
    protected void loadDataAsync(final @NotNull Runnable onLoaded) {
        final int generation = loadGeneration.incrementAndGet();
        loaded = false;
        list.setPaintBusy(true);
        list.getEmptyText().setText(Bundle.message("editor.loading"));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                indexer.awaitIndexing();
                final @NotNull TestRunDto fromDisk = run.orElseGet(() -> testRuns.getTestRunByPath(parent.getPath()));

                final @NotNull Map<UUID, TestRunItems> results = fromDisk.getResults().stream()
                        .collect(Collectors.toMap(TestRunItems::getId, item -> item,
                                (existingItem, _) -> existingItem));

                final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(fromDisk.getResults().stream().map(TestRunItems::liveTestCase).toList());
                testCaseValues.load(ordered);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != loadGeneration.get()) return;
                    run = Optional.of(fromDisk);
                    resultsMap.putAll(results);
                    allTestCases.clear();
                    allTestCases.addAll(ordered);
                    currentTestCases.clear();
                    currentTestCases.addAll(ordered);

                    jumpToPageOfPendingSelection();

                    list.setPaintBusy(false);
                    if (allTestCases.isEmpty()) {
                        list.getEmptyText().setText(Bundle.message("editor.run.empty"));
                    }
                    onExecutionStateChanged();
                    refreshView();
                    focusIfGoingTo();

                    loaded = true;
                    startIfAsked();
                    onLoaded.run();
                });
            } catch (final Exception ex) {
                Logger.error("Failed to load Test Run data from disk: " + FailureText.of(ex));
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != loadGeneration.get()) return;

                    list.setPaintBusy(false);
                    list.getEmptyText().setText(Bundle.message("editor.run.unreadable"));

                    startWhenLoaded = false;
                });
            }
        });
    }

    @Override
    protected @NotNull Done refreshed() {
        return walk.isExecuting() ? Done.REFRESHED_EXECUTION_STOPPED : Done.REFRESHED;
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    @Override
    protected void beforeReload() {
        walk.haltBeforeReload();
    }

    @Override
    protected void clearLoadedData() {
        resultsMap.clear();

        run = Optional.empty();
    }

    @Override
    protected void beforeDispose() {
        teardown(
                () -> lightMode.editorClosing(parent),

                () -> {
                    if (walk.isExecuting()) walk.stopAndWriteTheRunDown();
                },
                walk::dispose);
    }

    @Override
    protected void disposeLoadedData() {
        resultsMap.clear();
    }

    public @NotNull Optional<TestRunItems> runItem(final @NotNull UUID id) {
        return Optional.ofNullable(resultsMap.get(id));
    }

    public @NotNull Optional<TestRunDto> run() {
        return run;
    }

    private void buildOpeningPanel() {
        StatusBarListener.attach(this);

        list.setCellRenderer(new RunListRenderer(p, this));

        wireList();

        mainPanel.add(SideScroll.of(toolBar), BorderLayout.NORTH);
        mainPanel.add(SideScroll.of(statusBar), BorderLayout.SOUTH);
        toolBar.installSearchFocusShortcut(mainPanel);

        onToolBarSwitchedToListView();

        refreshView();
    }

    // UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-191
    @Override
    public void onToolBarResultAnalysisClicked() {
        run().ifPresent(runData -> new ResultAnalysisDialog(p,
                TestRunSummary.of(runData.getResults()),
                parent.getMarker().getResultAnalysis(),
                analysis -> {
                    testRuns.changeRunMarker(parent.getPath(),
                            marker -> marker.recordAnalysis(ResultAnalysis.written(analysis)));
                    notifier.softShow(p, Done.SAVED);
                }).show());
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014, Rule-EDITOR-PANEL-239
    @Override
    public @NotNull String cardTitle(final @NotNull TestCaseDto tc) {
        final @NotNull Set<RunEditorAttributes> selected = getSelectedDetails();
        final @NotNull TestCaseDto shown = runItem(tc.getId()).map(TestRunItems::shownTestCase).orElse(tc);

        return BaseCard.titleText(positionOf(tc),
                selected.contains(RunEditorAttributes.ORDER),
                selected.contains(RunEditorAttributes.DESCRIPTION) ? TestEditorAttributes.DESCRIPTION.displayValue(shown) : "");
    }

    @Override
    protected @NotNull RunEditorContextMenu buildContextMenu() {
        return new RunEditorContextMenu(p, this, parent, list);
    }

    @Override
    protected @NotNull Class<RunEditorAttributes> attributeType() {
        return RunEditorAttributes.class;
    }

    @Override
    protected @NotNull Class<TestRunDirectoryDto> nodeType() {
        return TestRunDirectoryDto.class;
    }

    @Override
    public @NotNull Set<RunEditorAttributes> getSelectedDetails() {
        return getToolBar().getToolbarItem(RunDetailsPopupBtn.class).getSelectedDetails();
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101
    @Override
    protected void drawStatus(final @NotNull PageWindow page, final int totalItems) {
        statusBar.updatePaginationState(page.page(), page.totalPages());

        automationState.read(p, snapshotOfAll(), this::refreshView);
    }

    @Override
    protected void afterSelectionShown() {
        showRunTotals();
    }

    // Rule-EDITOR-PANEL-135
    @Override
    protected void afterViewRefreshed() {
        onExecutionStateChanged();
    }

    @Override
    public boolean hasRunStatuses() {
        return true;
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-176
    public void refreshAfterRunStatusChanged() {
        list.repaint();
        statusBar.updatePaginationState(currentPage, getTotalPageCount());
        showRunTotals();
        onExecutionStateChanged();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
    @Override
    public @NotNull Set<String> getAvailableModules() {
        return Modules.in(snapshotOfAll());
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-094
    @Override
    protected @NotNull List<String[]> gridRows(final @NotNull List<TestCaseDto> pageItems) {
        return gridPanelBuilder.runRows(pageItems, resultsMap, this::positionOf);
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-094
    @Override
    protected @NotNull JBTable buildTable(final @NotNull List<String[]> rows, final @NotNull Set<RunEditorAttributes> attributes) {
        return gridPanelBuilder.buildRunTable(rows, attributes);
    }

    @Override
    protected void installEditListener(final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems) {
        table.getModel().addTableModelListener(new RunGridEditListener(p, this, pageItems, model::allContentsChanged));
    }

    @Override
    protected @NotNull List<TestCaseDto> getFilteredList() {
        final @NotNull EditorFilters filters = EditorFilters.of(toolBar);

        final @NotNull List<TestCaseDto> matched;
        synchronized (allTestCases) {
            matched = TestCaseFilter.filter(allTestCases, filters, this::runItem);
        }

        return automationState.matching(matched, filters.automation());
    }

    @Override
    public void updateSequenceAndSaveAll(final @NotNull Runnable onPersisted) {
        onPersisted.run();
    }

    @Override
    public void launching(final @NotNull UUID testCaseId) {
        walk.launching(testCaseId);
    }

    // UC-REPORT-001
    @Override
    public @NotNull Optional<TestRunDirectoryDto> shownRun() {
        return Optional.of(getParent());
    }

    // UC-EDITOR-PANEL-044
    @Override
    public void runWhenLoaded() {
        startWhenLoaded = true;
        startIfAsked();
    }

    private void startIfAsked() {
        if (!startWhenLoaded || !loaded) return;

        startWhenLoaded = false;
        walk.runPending();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130
    void showExecuting(final int globalIndex) {
        final int expectedPage = (globalIndex / pageSize) + 1;
        if (currentPage != expectedPage) {
            currentPage = expectedPage;
            refreshView();
        }

        final int localIndex = globalIndex - ((currentPage - 1) * pageSize);

        list.setSelectedIndex(localIndex);
        list.ensureIndexIsVisible(localIndex);
    }

    void repaint(final @NotNull TestCaseDto tc) {
        if (model.contains(tc)) model.contentsChanged(tc);
    }

    @NotNull List<TestRunItems> results() {
        return List.copyOf(resultsMap.values());
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-177
    void showRunTotals() {
        final @NotNull TestRunStatus status = parent.getMarker().getStatus();

        statusBar.showRunStatus(status);
        statusBar.showVerdicts(ResultAnalysis.segments(TestRunSummary.of(results()), status));

        showElapsed();
    }

    // UC-EDITOR-PANEL-042
    void showElapsed() {
        statusBar.showExecutionTime(Display.formatRunClock(getElapsed()));

        lightMode.tick(parent);
    }

    public @NotNull Duration getElapsed() {
        return resultsMap.values().stream()
                .map(TestRunItems::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119
    @Override
    public boolean isBusy() {
        return walk.isExecuting() || super.isBusy();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-133
    public void onExecutionStateChanged() {
        if (isDisposed()) return;

        final boolean executing = walk.isExecuting();

        final @NotNull StartExecutionBtn startBtn = toolBar.getToolbarItem(StartExecutionBtn.class);
        final @NotNull StopExecutionBtn stopBtn = toolBar.getToolbarItem(StopExecutionBtn.class);

        startBtn.setVisible(!executing);
        stopBtn.setVisible(executing);
        startBtn.updateEnabledState();
        toolBar.getToolbarItem(ResultAnalysisBtn.class).updateEnabledState();
        toolBar.getToolbarItem(GenerateReportBtn.class).updateEnabledState();
        toolBar.getToolbarItem(LightModeBtn.class).updateState();

        toolBar.revalidate();
        toolBar.repaint();

        lightMode.refresh(parent);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    @Override
    public void onStartExecutionClicked() {
        walk.start();
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-149
    @Override
    public void onStopExecutionClicked() {
        walk.stop();
    }
}
