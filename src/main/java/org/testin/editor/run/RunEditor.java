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
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.AutomationState;
import org.testin.editor.BaseCard;
import org.testin.editor.EditorFilters;
import org.testin.editor.PageWindow;
import org.testin.editor.TestCaseFilter;
import org.testin.editor.TestinEditor;
import org.testin.editor.UnifiedVirtualFile;
import org.testin.editor.listeners.RunGridEditListener;
import org.testin.editor.listeners.RunListRenderer;
import org.testin.editor.listeners.StatusBarListener;
import org.testin.editor.statusbar.StatusBar;
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
import org.testin.testrun.RunEditorAttributes;
import org.testin.model.RunStatus;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.runner.RunTestCases;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.TestCaseOrder;
import org.testin.testrun.ResultAnalysisDialog;
import org.testin.testrun.RunStatusService;
import org.testin.testrun.TestRunStatusChange;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.editor.AbstractTestinEditor;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RunEditor extends AbstractTestinEditor<RunEditorAttributes, TestRunDirectoryDto> implements Toolbar {

    /**
     * What the run recorded for each case, by case id. Not handed out: callers
     * ask {@link #runItem(UUID)}, so "this case has no run item" is one answer
     * rather than eight map lookups each checked for null (#71).
     */
    private final @NotNull Map<UUID, TestRunItems> resultsMap;

    private final @NotNull RunExecutionTimer executionTimer = new RunExecutionTimer();

    /**
     * Cases launched from this editor and not yet reported on.
     * <p>
     * An execution report names the case and nothing else, and the same case can
     * sit in several runs - so this is what tells this run that the tester
     * started that case here and not in the tab beside it.
     */
    private final @NotNull Set<UUID> launchedHere = ConcurrentHashMap.newKeySet();
    /**
     * Guards against a stale in-flight load overwriting a newer one (e.g. double refresh).
     */
    private final @NotNull AtomicInteger loadGeneration = new AtomicInteger();

    /**
     * The run being edited, and empty while a reload is replacing it. Volatile:
     * loaded off the EDT and read on it.
     */
    private volatile @NotNull Optional<TestRunDto> tr = Optional.empty();

    /**
     * UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126.
     * <p>
     * The same, telling {@code onLoaded} once the run is on screen - which is
     * the only moment a refresh can honestly be confirmed. The failure branch
     * says nothing, because nothing was refreshed.
     */
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
                // Snapshotted into a local: reading the volatile field twice would
                // let another thread empty it between the question and the answer.
                final @NotNull TestRunDto run = tr.orElseGet(() -> indexer.getTestRunByPath(parent.getPath()));
                tr = Optional.of(run);

                resultsMap.putAll(run.getResults().stream()
                        .collect(Collectors.toMap(TestRunItems::getId, item -> item,
                                (existingItem, duplicateItem) -> existingItem)));

                final @NotNull List<TestCaseDto> loadedItems = new ArrayList<>();
                for (final TestRunItems item : run.getResults()) {
                    final @NotNull Optional<TestCaseDto> indexed = indexer.findTestCase(item.getId());

                    // A case deleted since the run leaves its result behind, and
                    // the result is what the run is a record of. The row stays,
                    // says so, and takes the one status a tester cannot give.
                    //
                    // In memory only. The file heals the next time the run is
                    // written, the way the missing-stamp repair already does -
                    // opening a run rewrites nothing.
                    if (indexed.isEmpty()) {
                        Logger.warn("Test run references a deleted test case id=" + item.getId());
                        item.setStatus(TestStatus.REMOVED);
                    }

                    final @NotNull TestCaseDto testCase = indexed.orElseGet(() -> TestCaseDto.deleted(item.getId()));

                    loadedItems.add(testCase);
                    runItem(item.getId()).ifPresent(runItem -> runItem.setTc(testCase));
                }

                final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(loadedItems);
                Services.getInstance(p, TestCaseValues.class).load(ordered);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != loadGeneration.get()) return;
                    allTestCases.clear();
                    allTestCases.addAll(ordered);
                    currentTestCases.clear();
                    currentTestCases.addAll(ordered);

                    // Before refreshView reads currentPage: the reload may have moved
                    // the remembered test case onto a different page.
                    jumpToPageOfPendingSelection();

                    list.setPaintBusy(false);
                    if (allTestCases.isEmpty()) {
                        list.getEmptyText().setText(Bundle.message("editor.run.empty"));
                    }
                    // Also the first paint's answer: Stop starts hidden because a run
                    // that has just loaded is not executing.
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
                    // The same guard the success branch takes, and for the same
                    // reason. Without it a slow read that failed landed after a
                    // newer one that worked, wrote "Unable to read this test
                    // run" over a run the tester was already looking at, and
                    // cancelled the start they had asked for. TestEditor's
                    // failure branch has guarded since it was written (#66,
                    // finding 87).
                    if (generation != loadGeneration.get()) return;

                    list.setPaintBusy(false);
                    list.getEmptyText().setText(Bundle.message("editor.run.unreadable"));

                    // The request goes with the load that could not serve it.
                    startWhenLoaded = false;
                });
            }
        });
    }

    /**
     * A refresh reads the run again from disk and the walk goes with the old
     * copy, so the message has to carry both (#218).
     */
    @Override
    protected @NotNull Done refreshed() {
        return isExecuting() ? Done.REFRESHED_EXECUTION_STOPPED : Done.REFRESHED;
    }

    /**
     * The walk stops before the copy it is walking is thrown away.
     */
    @Override
    protected void beforeReload() {
        haltExecution();
    }

    /**
     * What the run recorded goes with the cases it was recorded against: both
     * are about to be read again, and a result map left behind would answer for
     * cases that are no longer the ones on screen.
     */
    @Override
    protected void clearLoadedData() {
        resultsMap.clear();

        tr = Optional.empty();
    }

    /**
     * Closing the tab is the same gesture as pressing Stop, so it does the same
     * thing: the automation this editor launched is ended, the walk is halted,
     * and the run is written.
     * <p>
     * Stopping the automation first, because halting the walk is what stops this
     * editor claiming to execute, and the cases to stop are read from that
     * claim. Without it the TestNG process outlived the tab, the cards kept
     * filling in, and every verdict it went on to report was written into no run
     * at all - the only subscription that could have recorded them went with
     * this editor (#222).
     */
    @Override
    protected void beforeDispose() {
        teardown(
                // The light mode window reads this editor every time it draws,
                // so it must not outlive it. Here rather than a Disposer child
                // registered on this editor, which never ran - this dispose is
                // called directly rather than through the Disposer - and which
                // quietly adopted the editor under the application root, where
                // nothing removed it and the IDE reported it as a leak on every
                // quit (#292).
                () -> Services.getInstance(p, LightMode.class).editorClosing(parent),

                this::stopAndWriteTheRunDown,
                executionTimer::dispose);
    }

    /**
     * The walk this editor was on, stopped and written down.
     * <p>
     * Its own step, because it is the one that can fail: {@code isExecuting()}
     * asks the service container for {@code TestNGExecution}, and during a
     * project close that is exactly the call that throws. Beside the others as
     * plain statements it took the timer and the light mode window with it
     * (#66, finding 70).
     */
    private void stopAndWriteTheRunDown() {
        if (!isExecuting()) return;

        stopAutomation();
        stopExecution();
        Services.getInstance(p, RunStatusService.class).persistRun(p, this);
    }

    @Override
    protected void disposeLoadedData() {
        resultsMap.clear();
    }

    /**
     * What the run recorded for this case, empty when it recorded nothing - a
     * case added to the test set after the run was created, or a map still being
     * refilled by a reload.
     */
    public @NotNull Optional<TestRunItems> runItem(final @NotNull UUID id) {
        return Optional.ofNullable(resultsMap.get(id));
    }

    /**
     * The run being edited, empty while a reload is replacing it.
     */
    public @NotNull Optional<TestRunDto> run() {
        return tr;
    }

    @Getter
    private int currentlyExecutingIndex = -1;

    /**
     * Whether the cases have been read.
     * <p>
     * Not answerable from {@code tr}, which is set on the pooled thread before the
     * cases it points at have been resolved - so an editor asked to start in that
     * window would have found an empty list and started nothing.
     */
    private boolean loaded;

    /**
     * Whether something asked this editor to start as soon as it could, and has
     * not been served yet. Cleared when it is served and when a load fails, so a
     * request that could not be met does not sit armed and fire on the next
     * unrelated reload - a toolbar Refresh, or a sync catching the editor up.
     */
    private boolean startWhenLoaded;

    public RunEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        super(p, vf.getTestRun());

        this.resultsMap = new ConcurrentHashMap<>();

        // The run editor hears about executions for the first time here. The
        // test editor and the view panel only ever repainted on a report; this
        // one records what the report said, because a run is where a verdict
        // belongs.
        TestCaseExecutionSubscriber.onReported(p, projectDisposable, this::executionReported);

        buildOpeningPanel();
        loadDataAsync();
    }

    private void buildOpeningPanel() {
        toolBar = new RunToolbar(p, this);
        statusBar = new StatusBar();
        StatusBarListener.attach(this);

        // Run editor specifics: the run card renderer.
        list.setCellRenderer(new RunListRenderer(p, this));

        wireList();

        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        toolBar.installSearchFocusShortcut(mainPanel);

        // List view is the default mode when the editor opens.
        onToolBarSwitchedToListView();

        refreshView();
    }

    /**
     * Refreshes status-dependent filtering without losing the page the user is viewing.
     */
    public void refreshAfterStatusChange() {
        refreshView();
    }

    /**
     * UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-191.
     * <p>
     * What the run means, in the tester's words, kept on the run itself.
     * <p>
     * The counts handed to the dialog are read from the results here rather than
     * stored with the text: they are derived, and a stored copy would be wrong
     * the moment a verdict changed.
     */
    @Override
    public void onToolBarResultAnalysisClicked() {
        run().ifPresent(runData -> new ResultAnalysisDialog(p,
                TestRunSummary.of(runData.getResults()),
                runData.getResultAnalysis(),
                analysis -> {
                    // Only the sections written in. Four empty strings were
                    // stored for the ones left alone, which every reader then
                    // treated as nothing anyway.
                    runData.setResultAnalysis(ResultAnalysis.written(analysis));
                    Services.getInstance(p, ProjectIndexer.class).putTestRun(parent.getPath(), runData);
                    Services.getInstance(p, Notifier.class).softShow(p, Done.SAVED);
                }).show());
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014
    @Override
    public @NotNull String cardTitle(final @NotNull TestCaseDto tc) {
        final @NotNull Set<RunEditorAttributes> selected = getSelectedDetails();

        return BaseCard.titleText(positionOf(tc),
                selected.contains(RunEditorAttributes.ORDER),
                selected.contains(RunEditorAttributes.DESCRIPTION) ? TestEditorAttributes.DESCRIPTION.displayValue(tc) : "");
    }

    @Override
    protected @NotNull RunEditorContextMenu buildContextMenu() {
        return new RunEditorContextMenu(p, this, parent, list, model);
    }

    @Override
    protected @NotNull Class<RunEditorAttributes> attributeType() {
        return RunEditorAttributes.class;
    }

    @Override
    public @NotNull Set<RunEditorAttributes> getSelectedDetails() {
        return getToolBar().getToolbarItem(RunDetailsPopupBtn.class).getSelectedDetails();
    }


    /**
     * UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101.
     * <p>
     * The page count, and how much of this run is automated.
     */
    @Override
    protected void drawStatus(final @NotNull PageWindow page, final int totalItems) {
        statusBar.updatePaginationState(page.page(), page.totalPages());

        // The same fired-and-forgotten read the test editor makes, into the same
        // service: a run holds test cases, and whether one has a generated method
        // is what says how much of this run will actually execute.
        Services.getInstance(p, AutomationState.class).read(p, snapshotOfAll(), this::refreshView);
    }

    /**
     * What the run has recorded so far, beside what is selected.
     */
    @Override
    protected void afterSelectionShown() {
        showRunTotals();
    }

    /**
     * What there is to walk is this list, so a filter that empties it grays
     * Start on the same redraw rather than at whatever happens next. Through the
     * one method that owns the buttons, so light mode's copy of them is told at
     * the same moment (Rule-EDITOR-PANEL-135).
     */
    @Override
    protected void afterViewRefreshed() {
        onExecutionStateChanged();
    }

    /**
     * A run editor draws run statuses; a test set editor does not.
     */
    public boolean hasRunStatuses() {
        return true;
    }

    /**
     * UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-176.
     * <p>
     * Redraws everything a run's status changes.
     * <p>
     * The cards carry the run's status, the page indicator is rebuilt with them,
     * and Start and Stop depend on whether the run is in progress. Here rather
     * than at the caller so the list stays this editor's own - the status change
     * used to be handed the list to repaint.
     */
    public void refreshAfterRunStatusChanged() {
        list.repaint();
        statusBar.updatePaginationState(currentPage, getTotalPageCount());
        // Not only the new status: completing a run turns every pending case into
        // untested, so the verdict counts beside it changed too and would have
        // stayed on the old numbers until the next redraw.
        showRunTotals();
        onExecutionStateChanged();
    }


    /**
     * UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095.
     * <p>
     * The modules this run's cases use, from {@link Modules} - which is what a
     * module is, the same way the groups below come from the owner that holds
     * those. Which cases to ask about is the editor's; what counts as a module
     * is not (#291).
     */
    @Override
    public @NotNull Set<String> getAvailableModules() {
        // Through the snapshot, not the live list: this is reached from an
        // ActionGroup the platform may build off the EDT while loadDataAsync is
        // between its clear() and its addAll() (#66, finding 84).
        return Modules.in(snapshotOfAll());
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-094
    @Override
    protected @NotNull JBTable buildTable(final @NotNull List<TestCaseDto> pageItems, final @NotNull Set<RunEditorAttributes> attributes) {
        return gridPanelBuilder.buildRunTable(p, pageItems, attributes, resultsMap, this::positionOf);
    }

    @Override
    protected void installEditListener(final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems) {
        // Typing into the Actual Result cell writes it to the run (#74).
        table.getModel().addTableModelListener(new RunGridEditListener(p, this, pageItems, model::allContentsChanged));
    }

    @Override
    protected @NotNull List<TestCaseDto> getFilteredList() {
        final @NotNull EditorFilters filters = EditorFilters.of(toolBar);
        // Status is the run editor's alone - a test case does not have one.
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
        // A run's order is the order it ran in, so there is nothing to write -
        // but the caller is still owed its continuation.
        onPersisted.run();
    }



    /**
     * UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-134.
     * <p>
     * Puts the walk on the next test case waiting for a verdict, at or after
     * {@code from}, and ends it when there is none.
     * <p>
     * The caller says where to look from, not where to land: every caller used to
     * hand in an exact row and every one of them could be wrong about it. The
     * verdict path passed the row after the one just judged, which is the next
     * row and not the next unjudged one - so a tester who ran a filtered set,
     * cleared the filter and pressed start again was walked onto test cases that
     * already carried a verdict, timed them a second time, and re-stamped who
     * judged them and when.
     */
    public void startTimerForIndex(final int from) {
        final int globalIndex = nextPendingIndex(from);

        if (globalIndex >= currentTestCases.size()) {
            // The end of this list is the end of the walk, and nothing more. The
            // list is the filtered one, so a tester who narrowed 120 cases to
            // three and judged all three used to complete the whole run and turn
            // the other 117 untested, with one balloon reading Completed (#214).
            // Whether the run is over is the run's own question, and it is asked
            // in one place.
            stopExecution();
            finishIfEverythingIsJudged();
            Services.getInstance(p, RunStatusService.class).persistRun(p, this);
            return;
        }

        currentlyExecutingIndex = globalIndex;

        final int expectedPage = (globalIndex / pageSize) + 1;
        if (currentPage != expectedPage) {
            currentPage = expectedPage;
            refreshView();
        }

        final int localIndex = globalIndex - ((currentPage - 1) * pageSize);

        list.setSelectedIndex(localIndex);
        list.ensureIndexIsVisible(localIndex);

        final @NotNull TestCaseDto currentTc = currentTestCases.get(globalIndex);

        // The index came from nextPendingIndex, so this case has a pending item -
        // the walk can land nowhere else. Read through the same Optional rather
        // than unwrapped, so a list that changed under the EDT ends the walk
        // instead of throwing in the middle of it.
        runItem(currentTc.getId()).ifPresent(item -> executionTimer.start(item, () -> {
            // A model event, not a repaint. The card grows a Duration line
            // the moment that value stops being blank, which makes the row
            // taller. JList re-measures a row only when the model says that row
            // changed: a repaint draws the taller content into the cached height
            // and clips it. That is why the duration stayed hidden until
            // toggling the attribute off and on forced the re-measure.
            //
            // Only when the case is on the page being viewed: contentsChanged
            // fires with index -1 for one that is not, which invalidates the
            // layout of the whole list once a second for a row nobody can see.
            if (model.contains(currentTc)) model.contentsChanged(currentTc);
            showElapsed();
        }));

        onExecutionStateChanged();
    }

    /**
     * UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130.
     * <p>
     * The first test case at or after {@code from} that is still waiting for a
     * verdict, and the size of the list when there is none - which is the value
     * {@link #startTimerForIndex(int)} reads as the end of the walk.
     * <p>
     * {@code PENDING} is the right question and the only one: the run owns that
     * status and clears it only when the run itself reaches a terminal state, so
     * it survives a stop, a filter and a reopen. A case with a verdict is not
     * pending, whoever gave it - the tester out of order, an automated run, or an
     * earlier walk - and a case the run does not hold at all is not pending
     * either.
     */
    public int nextPendingIndex(final int from) {
        for (int i = Math.max(from, 0); i < currentTestCases.size(); i++) {
            if (runItem(currentTestCases.get(i).getId())
                    .filter(item -> !item.isRemoved())
                    .filter(item -> item.getStatus() == TestStatus.PENDING)
                    .isPresent()) return i;
        }

        return currentTestCases.size();
    }

    /**
     * An execution reported on one of this run's cases.
     * <p>
     * A verdict is written into the run: the case takes its status, who ran it
     * and when. The tester clicked the run icon on a card and the run now holds
     * what happened, instead of the result living only on the badge the test
     * editor draws.
     * <p>
     * <b>The clock is not this method's.</b> Start Execution owns it, and it
     * times the case the tester is walking. Starting it again here would move it
     * onto whichever case the run icon was clicked on - and clicking one that is
     * not the current one would have taken the timer off the case actually being
     * executed and given its seconds to another.
     * <p>
     * A report about a case this run does not hold is not ours: the same case
     * can sit in several runs and be executed from any of them, and only the run
     * showing it records the result. A removed case records nothing at all.
     */
    @Override
    public void launching(final @NotNull UUID caseId) {
        launchedHere.add(caseId);

        markStartedByAutomation();
    }

    /**
     * UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-181.
     * <p>
     * A run with cases going has started, whoever started them.
     * <p>
     * The stamp used to belong to the Start Execution button, so a tester who
     * ran a case straight from a card got verdicts and durations written into a
     * run that still said it had never started - and every report printed a
     * blank start time for a run that plainly ran.
     * <p>
     * Once, not once per case: a selection of twelve calls this twelve times.
     * The stamp itself is idempotent, but the status change persists the marker
     * and tells the tester "In Progress", and twelve of those is eleven too
     * many. A run already in progress is left alone, and so is one signed off -
     * the same two conditions {@link #canStartExecution()} refuses on, because
     * this is the same question asked by a different gesture.
     */
    private void markStartedByAutomation() {
        final @NotNull TestRunStatus status = parent.getMarker().getStatus();
        if (status == TestRunStatus.IN_PROGRESS || status.isTerminal()) return;

        run().ifPresent(TestRunDto::markExecutionStarted);
        Services.getInstance(p, TestRunStatusChange.class).apply(parent, TestRunStatus.IN_PROGRESS);
    }

    /**
     * UC-EDITOR-PANEL-044.
     * <p>
     * Asked from outside to run what this run has left - see
     * {@link TestinEditor#runWhenLoaded()}.
     */
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

    /**
     * UC-EDITOR-PANEL-044, Rule-EDITOR-PANEL-184.
     * <p>
     * Runs the cases this run has not reached yet, and records their verdicts into
     * it.
     * <p>
     * Pending, not every case, and that is the same answer {@link #nextPendingIndex(int)}
     * gives the Start button beside it: re-running cases already judged puts a
     * tester back at the top of a run they were in the middle of, and it would
     * overwrite a verdict they gave by hand with one nothing asked for. A run with
     * nothing pending says so rather than quietly re-running all of it.
     * <p>
     * Cases already going are dropped here rather than left to {@link RunTestCases},
     * which drops them too - but silently, and after this has already claimed them.
     * A case claimed and then not started is a claim that never clears, so the next
     * verdict it does report, in whatever run actually ran it, is written into this
     * one as well.
     */
    private void runPending() {
        if (!canStartExecution()) {
            // The tree offers this because it cannot see inside the editor -
            // whether a run is mid-execution is known here and nowhere else. Said
            // rather than swallowed: the tab came forward and nothing happened,
            // which reads as a menu entry that does not work.
            if (isExecuting()) Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_RUNNING, parent.getName());
            return;
        }

        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        final @NotNull List<TestCaseDto> pending = snapshotOfAll().stream()
                .filter(tc -> runItem(tc.getId()).filter(item -> item.getStatus() == TestStatus.PENDING).isPresent())
                .filter(tc -> !execution.isRunning(tc.getId()))
                .toList();

        if (pending.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NOTHING_TO_RUN, parent.getName());
            return;
        }

        Logger.info("Running " + parent.getName() + " with " + pending.size() + " pending test case(s)");

        // Claimed before the launch, so every verdict that comes back lands in this
        // run rather than in whichever run editor happens to hold the same case.
        pending.forEach(tc -> launching(tc.getId()));

        RunTestCases.run(p, pending);
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-182
    private void executionReported(final @NotNull TestCaseDto tc, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        if (!launchedHere.contains(tc.getId())) return;

        // The claim is released by anything but "started": a verdict is the end
        // of the case, and IDLE is the runner declining it - no generated
        // method, indexing in the way - or a stop putting it back. Before the
        // guards below, because a run that has since been signed off, or a case
        // since deleted, still has to let go: a claim that outlives its
        // execution hands this run the next verdict that case earns in any
        // other run.
        if (!status.stillGoing()) launchedHere.remove(tc.getId());

        if (runItem(tc.getId()).filter(item -> !item.isRemoved()).isEmpty()) return;

        // A completed or closed run keeps what it recorded. It is signed off,
        // and an execution started from somewhere else must not rewrite its
        // history - which is the same rule that stops execution being started
        // on one at all.
        if (parent.getMarker().getStatus().isTerminal()) return;

        // Empty for a report that is not a verdict - a case that has just
        // started, or one a stop put back. A stop did not find a defect, so the
        // case keeps the status it had.
        //
        // Through RunStatusService rather than by writing the fields here: it
        // already owns recording a verdict, persisting the run, ending the
        // execution flow when the verdict is for the case being walked,
        // refreshing whichever view is showing, and confirming to the tester. A
        // verdict TestNG reached is the same verdict a tester would have typed,
        // so it takes the same path.
        status.getVerdict().ifPresent(verdict -> {
            sayWhatTheVerdictCleared(tc, verdict);

            Services.getInstance(p, RunStatusService.class).executeManual(p, this, tc, verdict, duration, failure);

            // Silent per case, and one line when the automation has nothing left
            // to report. Fifty cases used to raise fifty balloons, where every
            // other bulk gesture in Testin says one thing with a count (#219).
            if (launchedHere.isEmpty()) sayWhatTheRunRecorded();
        });

        // A model event, not a repaint: the card grows a Duration line the
        // moment that value stops being blank, and a JList re-measures a row
        // only when the model says that row changed.
        if (model.contains(tc)) model.contentsChanged(tc);
        showRunTotals();

        // Every report changes whether anything is still running, which is what
        // decides between Start and Stop. The first case reporting RUNNING is
        // what puts Stop up; the last verdict is what takes it down again.
        onExecutionStateChanged();

        finishIfEverythingIsJudged();
    }

    /**
     * UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-182.
     * <p>
     * Says what an automated verdict threw away, when it threw anything away.
     * <p>
     * A tester who fails a case and writes up why - the actual result, the
     * stacktrace, how bad the bug is - and then re-runs it, used to watch all
     * four fields vanish on a pass with nothing said. There is no copy of them
     * anywhere: not in the undo history, which records no run-item write, and not
     * on disk, which is overwritten a line later.
     * <p>
     * The keyboard path asks first and names exactly what is at stake. Automation
     * cannot ask: this runs on the EDT while the test process is still reporting,
     * so a modal here stalls it mid-run, and fifty cases would raise fifty dialogs
     * with nobody in front of them. So it says what it did instead (#220).
     * <p>
     * A notification rather than a balloon, because a run finishes on its own
     * time: a balloon fades while the tester is reading something else, and this
     * is the only record that the work existed.
     */
    private void sayWhatTheVerdictCleared(final @NotNull TestCaseDto tc, final @NotNull TestStatus verdict) {
        final @NotNull List<String> cleared = runItem(tc.getId()).map(item -> item.wouldClear(verdict)).orElseGet(List::of);
        if (cleared.isEmpty()) return;

        Services.getInstance(p, Notifier.class).info(p, Bundle.message("editor.cleared.title"),
                Bundle.message("editor.cleared.message", tc.getDescription(),
                        verdict.getLabel().toLowerCase(Locale.ROOT), Display.andJoin(cleared)));
    }

    /**
     * A run whose every case has a verdict is over, whoever gave them.
     * <p>
     * Completing one used to be the manual walk's business alone: the walk ran
     * off the end of the list and called it finished. So a tester who ran the
     * whole run through automation watched every card fill in and then found the
     * run still In Progress, with Start Execution offering to begin something
     * that had already happened.
     * <p>
     * Asked of the run rather than of the walk, so the answer does not depend on
     * which of the two executed it. A case deleted from the test set counts as
     * judged - the run keeps what it recorded about it and it can never be run
     * again, so waiting for it would be waiting forever.
     * <p>
     * A run already signed off is left alone: the report that reached this was
     * refused above.
     */
    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-134
    /**
     * UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-008.
     * <p>
     * What the automation recorded, in one line at the end of it - "Passed 42,
     * Failed 8".
     * <p>
     * The words are {@link ResultAnalysis#segments}, which the status bar
     * already uses, so the balloon and the bar cannot end up counting the same
     * run differently.
     */
    private void sayWhatTheRunRecorded() {
        final @NotNull String recorded = ResultAnalysis
                .segments(TestRunSummary.of(List.copyOf(resultsMap.values())), parent.getMarker().getStatus())
                .stream().map(ResultAnalysis.Segment::text).collect(Collectors.joining(", "));

        if (!recorded.isEmpty()) Services.getInstance(p, Notifier.class).softShow(p, recorded);
    }

    public void finishIfEverythingIsJudged() {
        // A run already signed off is left alone. The guard is here rather than
        // at the call sites because there are now five of them - the end of a
        // walk, an automated verdict, and the three ways a tester records one by
        // hand - and a run completed twice says Completed twice (#217).
        if (parent.getMarker().getStatus().isTerminal()) return;

        if (run().filter(TestRunDto::isFullyJudged).isEmpty()) return;

        Services.getInstance(p, TestRunStatusChange.class).apply(parent, TestRunStatus.COMPLETED);
    }

    /**
     * UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-177.
     * <p>
     * Puts what the run has done so far into the status bar: how many cases carry
     * each verdict, and how long they took.
     * <p>
     * One method for the two figures because they have one set of call sites -
     * the redraw, and the two points where a verdict lands - and a second figure
     * pushed from a subset of them would be current on some screens and stale on
     * others. That is the mistake {@code refreshView} records above, where four
     * callers hand-copied the same two lines.
     * <p>
     * The counts are read from the live results rather than the run on disk, so
     * the bar moves as the tester works rather than at the next save. The
     * phrasing is not decided here: {@link ResultAnalysis#segments} owns it, so
     * the bar and the reports cannot disagree about how many passed. It is given
     * the run's status as well as its results, because one of the four buckets is
     * named for it - untouched cases are pending until the run gives up on them.
     * <p>
     * The time is the sum of what the cases measured, not a clock of its own: it
     * counts only while a case is being timed, so a stop freezes it, a resuming
     * continues it, and it is back after a reopen because the case durations are.
     * A run judged from the context menu has measured nothing and shows blank, as
     * its cases do.
     */
    private void showRunTotals() {
        final @NotNull TestRunStatus status = parent.getMarker().getStatus();

        statusBar.showRunStatus(status);
        statusBar.showVerdicts(ResultAnalysis.segments(TestRunSummary.of(List.copyOf(resultsMap.values())), status));

        showElapsed();
    }

    /**
     * UC-EDITOR-PANEL-042.
     * <p>
     * The running total, and nothing else.
     * <p>
     * Separate because it alone changes every second. The timer ticks once a
     * second while a case is being timed, and it used to run the whole of
     * {@link #showRunTotals()} on each tick - summarizing the results, building
     * the verdict line and handing an html string to a label, which rebuilds its
     * view tree to accept it, all on the painting thread, for two figures that
     * cannot have changed since the tick before. A verdict is an event and is
     * pushed as one; the clock is not.
     */
    private void showElapsed() {
        statusBar.showExecutionTime(Display.formatRunClock(getElapsed()));

        // The clocks and nothing else, for the reason above: light mode redraws
        // its two figures here rather than through onExecutionStateChanged,
        // which re-measures and re-sizes the whole window.
        Services.getInstance(p, LightMode.class).tick(parent);
    }

    /**
     * How long this run has taken: every case's duration added up, including
     * the one being timed right now, which the timer writes to as it ticks.
     */
    public @NotNull Duration getElapsed() {
        return resultsMap.values().stream()
                .map(TestRunItems::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    /**
     * UC-EDITOR-PANEL-046.
     * <p>
     * How long the case being executed has taken so far, and zero when none is
     * - which is the honest answer rather than a missing one, and the same
     * value a case that has just started carries.
     */
    public @NotNull Duration getCurrentCaseElapsed() {
        if (currentlyExecutingIndex < 0 || currentlyExecutingIndex >= currentTestCases.size()) return Duration.ZERO;

        return runItem(currentTestCases.get(currentlyExecutingIndex).getId())
                .map(TestRunItems::getDuration)
                .orElse(Duration.ZERO);
    }

    /**
     * True while test cases are being executed, by either of the two things
     * that execute them: the tester walking the run by hand, and automation
     * this editor started.
     * <p>
     * It meant only the first, so a run driven entirely by the run icon offered
     * Start throughout - a button to begin something that was already going -
     * and never offered Stop. It also left {@link #isBusy()} false, so a refresh
     * from disk was free to reload the editor while its cases were still
     * reporting into it.
     * <p>
     * Asked of the runner rather than kept as a flag here. The executing index
     * and the runner's registry are each already the answer for their half, and
     * a third copy would be the thing that drifts - which is how a Stop button
     * comes to sit on a finished run.
     */
    public boolean isExecuting() {
        return currentlyExecutingIndex >= 0 || isAutomationRunning();
    }

    /**
     * Whether anything this editor launched is still going. Only what it
     * launched: a case can be in several open runs, and another run's execution
     * is not this one's to report or to stop.
     */
    private boolean isAutomationRunning() {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        return launchedHere.stream().anyMatch(execution::isRunning);
    }

    /**
     * UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119.
     * <p>
     * Busy while a run is being executed or a grid cell is open for editing -
     * either is live state that a reload under the tester would throw away, so an
     * on-disk refresh leaves this editor be until it is done (#20, #74).
     */
    @Override
    public boolean isBusy() {
        return isExecuting() || super.isBusy();
    }

    /**
     * Whether execution may start. Asked by both the toolbar button and the
     * context menu action, which used to answer it differently.
     */
    public boolean canStartExecution() {
        return !isExecuting() && !parent.getMarker().getStatus().isTerminal();
    }

    /**
     * UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135.
     * <p>
     * Whether the walk would land anywhere at all.
     * <p>
     * Three things arrive here and they are one thing to the walk: a test run
     * holding no test cases, a filter that matches nothing, and a list whose test
     * cases have all been judged. In each of them the walk would end on the step
     * it starts.
     * <p>
     * Asked of the filtered list because that is what the walk reads. Deliberately
     * not folded into {@link #canStartExecution()}, which the automation asks too -
     * automation runs the whole run rather than the filtered page, so a filter
     * hiding everything must not stop it (#215).
     */
    public boolean hasSomethingToWalk() {
        return nextPendingIndex(0) < currentTestCases.size();
    }

    /**
     * UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135.
     * <p>
     * The manual button's whole question, in one place, so the toolbar and light
     * mode cannot answer it differently - which is the mistake
     * {@link #canStartExecution()} above already records.
     */
    public boolean canStartManualExecution() {
        return canStartExecution() && hasSomethingToWalk();
    }

    /**
     * UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-133.
     * <p>
     * The one place that reacts to this run's execution changing: which of the
     * two execution buttons is showing, and what the light mode window is
     * drawing.
     * <p>
     * Start when idle, Stop while a run is under way, exactly as the list and grid
     * view buttons swap. It reads {@link #isExecuting()} rather than a flag of its
     * own: the executing index is already the answer, and a second copy would be a
     * second thing to keep in step - the one that drifted would leave a Stop button
     * on a finished run.
     * <p>
     * Named for the event rather than for the buttons since light mode joined it
     * (#13). A second window showing the case being executed needs telling at
     * exactly the moments the buttons do, and the alternative was a second hook
     * beside this one that the next change to the execution flow would update
     * only one of.
     * <p>
     * Called after every execution-state change, so nowhere else asks.
     */
    public void onExecutionStateChanged() {
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

    /**
     * UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-151.
     * <p>
     * Ends the execution flow, wherever the end came from - the tester's Stop, the
     * last verdict, a bulk apply, the run completing. The run itself decides
     * whether it has an end to stamp: a run nobody started has none.
     * <p>
     * The caller persists. Every path that reaches this already writes the run
     * afterward, so the stamp and the in-flight case's duration land in the file
     * together.
     */
    public void stopExecution() {
        tr.ifPresent(TestRunDto::markExecutionEnded);

        haltExecution();
    }

    /**
     * UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-152.
     * <p>
     * Stops whatever automation this editor started, and says how much went
     * back.
     * <p>
     * The button showed while automation ran and halted only the manual walk,
     * which during an automated run is not going - so it was a Stop that did
     * nothing when pressed, the same defect the card's own stop icon had (#34).
     * <p>
     * The runner reports more cases than were asked for when they share a
     * process, which is the honest count: stopping one case of twelve in one
     * configuration stops all twelve, and every one of them is put back.
     */
    private void stopAutomation() {
        if (launchedHere.isEmpty()) return;

        final int stopped = Services.getInstance(p, TestNGExecution.class).stopCases(launchedHere);
        if (stopped == 0) return;

        Logger.info("Stopped " + stopped + " test case(s) running from '" + parent.getName() + "'");
    }

    /**
     * UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150.
     * <p>
     * Stops the execution flow without saying the run ended.
     * <p>
     * Refresh needs exactly this. It throws the loaded run and its results away
     * and reads them again, and the timer used to survive that: it kept ticking
     * an item that was no longer in the map, so that case's duration went
     * nowhere, and the reloaded run still showed the Stop button for an
     * execution nothing was driving. What it must not do is stamp the run's end
     * - refreshing is not finishing, and a run refreshed halfway would report an
     * end time the tester never asked for.
     */
    private void haltExecution() {
        executionTimer.stop();
        currentlyExecutingIndex = -1;
        onExecutionStateChanged();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    @Override
    public void onStartExecutionClicked() {
        final @NotNull Optional<TestRunDto> run = run();
        if (run.isEmpty()) return;

        // Said rather than swallowed, and here rather than only on the button:
        // light mode's own start button calls this straight, and it grays
        // nothing. Pressing start with nothing to walk used to mark the test run
        // In Progress, stamp when execution began, and - until #214 - complete
        // the whole run at once (#215).
        if (!hasSomethingToWalk()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NOTHING_SHOWING, parent.getName());
            return;
        }

        // Before the status change, which is what persists the run.
        run.get().markExecutionStarted();
        Services.getInstance(p, TestRunStatusChange.class).apply(parent, TestRunStatus.IN_PROGRESS);
        // From the top, not from a row worked out here: where the walk lands is
        // the walk's own question, and it is answered in one place.
        startTimerForIndex(0);
    }

    /**
     * UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-149.
     * <p>
     * The tester's own stop, and the only one that reaches the test runner.
     * <p>
     * {@link #stopExecution()} runs on four internal paths - the last verdict, a
     * bulk apply, the run completing - where nobody pressed anything and a test
     * that is running is running legitimately. Killing it there would end a run
     * the tester never asked to end.
     */
    @Override
    public void onStopExecutionClicked() {
        // Before the walk is halted, because halting it is what stops this
        // editor claiming to be executing - and the cases to stop are read from
        // what it launched.
        stopAutomation();
        stopExecution();

        // The other stop paths persist as part of the verdict or status change they
        // belong to; this one is the tester's alone, so it writes the run itself -
        // the case duration ticked so far and the end stamp would otherwise live only
        // until the editor closed.
        Services.getInstance(p, RunStatusService.class).persistRun(p, this);
        Services.getInstance(p, Notifier.class).softShow(p, Done.STOPPED);
    }

}
