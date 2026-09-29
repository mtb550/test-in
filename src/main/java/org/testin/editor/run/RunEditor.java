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
import org.testin.codegen.AutomationState;
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
import org.testin.editor.toolbar.components.FilterPopupBtn;
import org.testin.editor.toolbar.components.GenerateReportBtn;
import org.testin.editor.toolbar.components.LightModeBtn;
import org.testin.editor.toolbar.components.ResultAnalysisBtn;
import org.testin.editor.toolbar.components.RunDetailsPopupBtn;
import org.testin.editor.toolbar.components.StartExecutionBtn;
import org.testin.editor.toolbar.components.StopExecutionBtn;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.TestRuns;
import org.testin.lightmode.LightMode;
import org.testin.logger.Logger;
import org.testin.model.Modules;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testrun.ResultAnalysisDialog;
import org.testin.testrun.RunEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.Display;

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
    private final @NotNull Map<UUID, TestRunItems> resultsMap = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull RunToolbar toolBar;

    @Getter
    private final @NotNull RunWalk walk = new RunWalk(this, p, parent);

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
                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
                indexer.awaitIndexing();
                final @NotNull TestRunDto fromDisk = run.orElseGet(() -> testRuns.getTestRunByPath(parent.getPath()));

                final @NotNull Map<UUID, TestRunItems> results = fromDisk.getResults().stream()
                        .collect(Collectors.toMap(TestRunItems::getId, item -> item,
                                (existingItem, _) -> existingItem));

                final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(fromDisk.getResults().stream().map(TestRunItems::liveTestCase).toList());
                Services.getInstance(p, TestCaseValues.class).load(ordered);

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
                Logger.error("Failed to load Test Run data from disk: " + ex.getMessage());
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
                () -> Services.getInstance(p, LightMode.class).editorClosing(parent),

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

        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
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
                    Services.getInstance(p, TestRuns.class).changeRunMarker(parent.getPath(),
                            marker -> marker.recordAnalysis(ResultAnalysis.written(analysis)));
                    Services.getInstance(p, Notifier.class).softShow(p, Done.SAVED);
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
    public @NotNull Set<RunEditorAttributes> getSelectedDetails() {
        return getToolBar().getToolbarItem(RunDetailsPopupBtn.class).getSelectedDetails();
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101
    @Override
    protected void drawStatus(final @NotNull PageWindow page, final int totalItems) {
        statusBar.updatePaginationState(page.page(), page.totalPages());

        Services.getInstance(p, AutomationState.class).read(p, snapshotOfAll(), this::refreshView);
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
        final @NotNull Set<TestStatus> statusFilter = toolBar.getToolbarItem(FilterPopupBtn.class).getSelectedStatus();

        final @NotNull List<TestCaseDto> matched;
        synchronized (allTestCases) {
            matched = TestCaseFilter.filter(
                    allTestCases,
                    filters.query(),
                    filters.groups(),
                    filters.priorities(),
                    filters.modules(),
                    statusFilter,
                    this::runItem);
        }

        return Services.getInstance(p, AutomationState.class).matching(matched, filters.automation());
    }

    @Override
    public void updateSequenceAndSaveAll(final @NotNull Runnable onPersisted) {
        onPersisted.run();
    }

    @Override
    public void launching(final @NotNull UUID testCaseId) {
        walk.launching(testCaseId);
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

        Services.getInstance(p, LightMode.class).tick(parent);
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

        Services.getInstance(p, LightMode.class).refresh(parent);
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
