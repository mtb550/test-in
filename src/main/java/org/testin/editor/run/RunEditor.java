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
import org.testin.lightmode.LightMode;
import org.testin.logger.Logger;
import org.testin.model.Failure;
import org.testin.model.Modules;
import org.testin.model.ResultAnalysis;
import org.testin.model.RunStatus;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.runner.RunTestCases;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testrun.ResultAnalysisDialog;
import org.testin.testrun.RunEditorAttributes;
import org.testin.testrun.RunStatusService;
import org.testin.testrun.TestRunStatusChange;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.awt.BorderLayout;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
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

    private final @NotNull RunExecutionTimer executionTimer = new RunExecutionTimer();

    private final @NotNull Set<UUID> launchedHere = ConcurrentHashMap.newKeySet();
    private final @NotNull AtomicInteger loadGeneration = new AtomicInteger();

    private volatile @NotNull Optional<TestRunDto> run = Optional.empty();

    private @NotNull Optional<UUID> executingCase = Optional.empty();

    private boolean loaded;

    private boolean startWhenLoaded;

    public RunEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        super(p, vf.getTestRun());

        TestCaseExecutionSubscriber.onReported(p, projectDisposable, this::executionReported);

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
                indexer.awaitIndexing();
                final @NotNull TestRunDto fromDisk = run.orElseGet(() -> indexer.getTestRunByPath(parent.getPath()));

                final @NotNull Map<UUID, TestRunItems> results = fromDisk.getResults().stream()
                        .collect(Collectors.toMap(TestRunItems::getId, item -> item,
                                (existingItem, duplicateItem) -> existingItem));

                final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(fromDisk.getResults().stream().map(TestRunItems::liveCase).toList());
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
        return isExecuting() ? Done.REFRESHED_EXECUTION_STOPPED : Done.REFRESHED;
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    @Override
    protected void beforeReload() {
        final boolean timing = executingCase.isPresent();

        haltExecution();
        if (timing) saveRun();
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
                    if (isExecuting()) stopAndWriteTheRunDown();
                },
                executionTimer::dispose);
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-149
    private void stopAndWriteTheRunDown() {
        stopAutomation();
        stopExecution();
        saveRun();
    }

    private void saveRun() {
        Services.getInstance(p, ProjectIndexer.class).saveRun(parent.getPath());
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

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-227
    public int getCurrentlyExecutingIndex() {
        return executingCase.map(id -> {
            for (int i = 0; i < currentTestCases.size(); i++) {
                if (currentTestCases.get(i).getId().equals(id)) return i;
            }
            return -1;
        }).orElse(-1);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-227
    public boolean executingCaseIsHidden() {
        return executingCase.isPresent() && getCurrentlyExecutingIndex() == -1;
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
                    Services.getInstance(p, ProjectIndexer.class).changeRunMarker(parent.getPath(),
                            marker -> marker.setResultAnalysis(ResultAnalysis.written(analysis)));
                    Services.getInstance(p, Notifier.class).softShow(p, Done.SAVED);
                }).show());
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014, Rule-EDITOR-PANEL-239
    @Override
    public @NotNull String cardTitle(final @NotNull TestCaseDto tc) {
        final @NotNull Set<RunEditorAttributes> selected = getSelectedDetails();
        final @NotNull TestCaseDto shown = runItem(tc.getId()).map(TestRunItems::shownCase).orElse(tc);

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
    protected @NotNull JBTable buildTable(final @NotNull List<TestCaseDto> pageItems, final @NotNull Set<RunEditorAttributes> attributes) {
        return gridPanelBuilder.buildRunTable(pageItems, attributes, resultsMap, this::positionOf);
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

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-134
    public void startTimerForIndex(final int from) {
        final int globalIndex = nextPendingIndex(from);

        if (globalIndex >= currentTestCases.size()) {
            stopExecution();
            finishIfEverythingIsJudged();
            saveRun();
            return;
        }

        executingCase = Optional.of(currentTestCases.get(globalIndex).getId());

        final int expectedPage = (globalIndex / pageSize) + 1;
        if (currentPage != expectedPage) {
            currentPage = expectedPage;
            refreshView();
        }

        final int localIndex = globalIndex - ((currentPage - 1) * pageSize);

        list.setSelectedIndex(localIndex);
        list.ensureIndexIsVisible(localIndex);

        final @NotNull TestCaseDto currentTc = currentTestCases.get(globalIndex);

        runItem(currentTc.getId()).ifPresent(item -> executionTimer.start(item, () -> {
            if (model.contains(currentTc)) model.contentsChanged(currentTc);
            showElapsed();
        }));

        onExecutionStateChanged();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130
    public int nextPendingIndex(final int from) {
        for (int i = Math.max(from, 0); i < currentTestCases.size(); i++) {
            if (runItem(currentTestCases.get(i).getId())
                    .filter(item -> item.shownStatus() == TestStatus.PENDING)
                    .isPresent()) return i;
        }

        return currentTestCases.size();
    }

    @Override
    public void launching(final @NotNull UUID caseId) {
        launchedHere.add(caseId);

        markStartedByAutomation();
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-181
    private void markStartedByAutomation() {
        final @NotNull TestRunStatus status = parent.getMarker().getStatus();
        if (status == TestRunStatus.IN_PROGRESS || status.isTerminal()) return;

        markStarted();
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-043
    private void markStarted() {
        Services.getInstance(p, ProjectIndexer.class).changeRunMarker(parent.getPath(), TestRunMarker::markExecutionStarted);
        Services.getInstance(p, TestRunStatusChange.class).apply(parent, TestRunStatus.IN_PROGRESS);
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
        runPending();
    }

    // UC-EDITOR-PANEL-044, Rule-EDITOR-PANEL-184
    private void runPending() {
        if (!canStartExecution()) {
            if (isExecuting())
                Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_RUNNING, parent.getName());
            return;
        }

        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        final @NotNull List<TestCaseDto> pending = snapshotOfAll().stream()
                .filter(tc -> runItem(tc.getId()).filter(item -> item.shownStatus() == TestStatus.PENDING).isPresent())
                .filter(tc -> !execution.isRunning(tc.getId()))
                .toList();

        if (pending.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NOTHING_TO_RUN, parent.getName());
            return;
        }

        Logger.info("Running " + parent.getName() + " with " + pending.size() + " pending test case(s)");

        pending.forEach(tc -> launching(tc.getId()));

        RunTestCases.run(p, pending);
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-182
    private void executionReported(final @NotNull TestCaseDto tc, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        if (!launchedHere.contains(tc.getId())) return;

        if (!status.stillGoing()) launchedHere.remove(tc.getId());

        if (runItem(tc.getId()).filter(item -> !item.isRemoved()).isEmpty()) return;

        if (parent.getMarker().getStatus().isTerminal()) return;

        status.getVerdict().ifPresent(verdict -> {
            sayWhatTheVerdictCleared(tc, verdict, failure);

            Services.getInstance(p, RunStatusService.class).recordReported(p, this, tc, verdict, duration, failure);

            if (launchedHere.isEmpty()) sayWhatTheRunRecorded();
        });

        if (model.contains(tc)) model.contentsChanged(tc);
        showRunTotals();

        onExecutionStateChanged();

        finishIfEverythingIsJudged();
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-182, Rule-EDITOR-PANEL-220
    private void sayWhatTheVerdictCleared(final @NotNull TestCaseDto tc, final @NotNull TestStatus verdict, final @NotNull Failure failure) {
        final @NotNull List<String> cleared = runItem(tc.getId()).map(item -> item.wouldClear(verdict, failure)).orElseGet(List::of);
        if (cleared.isEmpty()) return;

        Services.getInstance(p, Notifier.class).info(p, Bundle.message("editor.cleared.title"),
                Bundle.message("editor.cleared.message", tc.getDescription(),
                        verdict.getLabel().toLowerCase(Locale.ROOT), Display.andJoin(cleared)));
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-008
    private void sayWhatTheRunRecorded() {
        final @NotNull String recorded = ResultAnalysis
                .segments(TestRunSummary.of(List.copyOf(resultsMap.values())), parent.getMarker().getStatus())
                .stream().map(ResultAnalysis.Segment::text).collect(Collectors.joining(", "));

        if (!recorded.isEmpty()) Services.getInstance(p, Notifier.class).softShow(p, recorded);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-134
    public void finishIfEverythingIsJudged() {
        if (parent.getMarker().getStatus().isTerminal()) return;

        if (run().filter(TestRunDto::isFullyJudged).isEmpty()) return;

        Services.getInstance(p, TestRunStatusChange.class).apply(parent, TestRunStatus.COMPLETED);
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-177
    private void showRunTotals() {
        final @NotNull TestRunStatus status = parent.getMarker().getStatus();

        statusBar.showRunStatus(status);
        statusBar.showVerdicts(ResultAnalysis.segments(TestRunSummary.of(List.copyOf(resultsMap.values())), status));

        showElapsed();
    }

    // UC-EDITOR-PANEL-042
    private void showElapsed() {
        statusBar.showExecutionTime(Display.formatRunClock(getElapsed()));

        Services.getInstance(p, LightMode.class).tick(parent);
    }

    public @NotNull Duration getElapsed() {
        return resultsMap.values().stream()
                .map(TestRunItems::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    // UC-EDITOR-PANEL-046
    public @NotNull Duration getCurrentCaseElapsed() {
        return executingCase.flatMap(this::runItem)
                .map(TestRunItems::getDuration)
                .orElse(Duration.ZERO);
    }

    public boolean isExecuting() {
        return executingCase.isPresent() || isAutomationRunning();
    }

    private boolean isAutomationRunning() {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        return launchedHere.stream().anyMatch(execution::isRunning);
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119
    @Override
    public boolean isBusy() {
        return isExecuting() || super.isBusy();
    }

    public boolean canStartExecution() {
        return !isExecuting() && !parent.getMarker().getStatus().isTerminal();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public boolean hasSomethingToWalk() {
        return nextPendingIndex(0) < currentTestCases.size();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public boolean canStartManualExecution() {
        return canStartExecution() && hasSomethingToWalk();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-133
    public void onExecutionStateChanged() {
        if (isDisposed()) return;

        final boolean executing = isExecuting();

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

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-151
    public void stopExecution() {
        Services.getInstance(p, ProjectIndexer.class).changeRunMarker(parent.getPath(), TestRunMarker::markExecutionEnded);

        haltExecution();
    }

    // UC-EDITOR-PANEL-039, Rule-EDITOR-PANEL-164
    public void stopExecutionUntimed() {
        executionTimer.discard();
        stopExecution();
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-152
    private void stopAutomation() {
        if (launchedHere.isEmpty()) return;

        final int stopped = Services.getInstance(p, TestNGExecution.class).stopCases(launchedHere);
        if (stopped == 0) return;

        Logger.info("Stopped " + stopped + " test case(s) running from '" + parent.getName() + "'");
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    private void haltExecution() {
        executionTimer.stop();
        executingCase = Optional.empty();
        onExecutionStateChanged();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    @Override
    public void onStartExecutionClicked() {
        if (run.isEmpty()) return;

        if (!hasSomethingToWalk()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NOTHING_SHOWING, parent.getName());
            return;
        }

        markStarted();
        startTimerForIndex(0);
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-149
    @Override
    public void onStopExecutionClicked() {
        stopAndWriteTheRunDown();

        Services.getInstance(p, Notifier.class).softShow(p, Done.STOPPED);
    }
}
