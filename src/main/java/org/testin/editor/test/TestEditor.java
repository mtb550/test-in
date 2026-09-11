package org.testin.editor.test;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.BaseCard;
import org.testin.editor.EditorFilters;
import org.testin.editor.PageWindow;
import org.testin.editor.TestCaseFilter;
import org.testin.editor.UnifiedVirtualFile;
import org.testin.editor.listeners.GridEditListener;
import org.testin.editor.listeners.ModelChangeNotifier;
import org.testin.editor.listeners.StatusBarListener;
import org.testin.editor.listeners.TestListRenderer;
import org.testin.editor.listeners.TransferListener;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.toolbar.TestToolbar;
import org.testin.editor.toolbar.Toolbar;
import org.testin.editor.toolbar.components.TestDetailsPopupBtn;
import org.testin.codegen.AutomationState;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.model.Modules;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseAction;
import org.testin.testcase.TestCaseOrder;
import org.testin.editor.AbstractTestinEditor;
import org.testin.util.Bundle;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestEditor extends AbstractTestinEditor<TestEditorAttributes, TestSetDirectoryDto> implements Toolbar {


    private final @NotNull ModelChangeNotifier modelChangeNotifier;
    /**
     * One counter for every model-replacing operation - data loads and badge
     * sorts alike (#24). Each one bumps and checks it, so a stale in-flight
     * result never overwrites a newer one, whichever kind it is.
     */
    private final @NotNull AtomicInteger modelGeneration = new AtomicInteger();

    /**
     * True from the moment a load starts until its data is on screen. The empty
     * message asks it, so a list that is empty because it is still loading keeps
     * the loading message instead of being told there is nothing to show.
     */
    private volatile boolean loading;

    public TestEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        super(p, vf.getTestSet());

        // Test editor specifics: manual reordering by drag-and-drop and the card renderer.
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new TransferListener(p, this));
        list.setCellRenderer(new TestListRenderer(p, this));

        this.toolBar = new TestToolbar(this);
        mainPanel.add(toolBar, BorderLayout.NORTH);
        toolBar.installSearchFocusShortcut(mainPanel);

        this.modelChangeNotifier = new ModelChangeNotifier();
        this.modelChangeNotifier.setOnUpdateCallback(this::onDataSynced);
        this.model.addListDataListener(modelChangeNotifier);

        wireList();

        this.statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        StatusBarListener.attach(this);

        TestCaseExecutionSubscriber.onReported(p, projectDisposable, (tc, status, duration, failure) -> list.repaint());

        // List view is the default mode when the editor opens.
        onToolBarSwitchedToListView();

        loadDataAsync();
    }



    /**
     * The same, telling {@code onLoaded} once the cases are on screen - which is
     * the only moment a refresh can honestly be confirmed.
     */
    @Override
    protected void loadDataAsync(final @NotNull Runnable onLoaded) {
        final int generation = modelGeneration.incrementAndGet();
        loading = true;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                indexer.awaitIndexing();

                final @NotNull List<TestCaseDto> items = indexer.getTestCasesForTestSet(parent.getPath());

                if (items.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (generation != modelGeneration.get()) return;
                        allTestCases.clear();
                        currentTestCases.clear();
                        list.setPaintBusy(false);
                        loading = false;
                        // The message comes from refreshView, which is the one place
                        // that knows what the page ended up holding.
                        refreshView();
                        onLoaded.run();
                    });
                    return;
                }

                Services.getInstance(p, TestCaseValues.class).load(items);

                final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(items);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != modelGeneration.get()) return;
                    allTestCases.clear();
                    allTestCases.addAll(ordered);
                    currentTestCases.clear();
                    currentTestCases.addAll(ordered);

                    ordered.forEach(tc -> tc.setParent(parent));

                    // The item may now sit on a different page than before the reload.
                    jumpToPageOfPendingSelection();

                    list.setPaintBusy(false);
                    loading = false;

                    refreshView();
                    focusIfGoingTo();
                    onLoaded.run();
                });

            // A read that throws used to leave the editor saying Loading for the
            // rest of the session: the pooled body had no catch, so nothing
            // cleared the flag, nothing painted, and the only trace was whatever
            // the platform logged about an uncaught exception. The run editor has
            // said what happened all along; this is the same thing on this side
            // (#66).
            } catch (final Exception ex) {
                Logger.error("Failed to load test set data from disk: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != modelGeneration.get()) return;

                    list.setPaintBusy(false);
                    loading = false;
                    list.getEmptyText().setText(Bundle.message("editor.test.unreadable"));
                });
            }
        });
    }

    private void onDataSynced() {
        orderThen(this::refreshView);
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-060
    @Override
    public void updateSequenceAndSaveAll(final @NotNull Runnable onPersisted) {
        final List<TestCaseDto> snapshot;
        synchronized (this.allTestCases) {
            snapshot = new ArrayList<>(this.allTestCases);
        }

        final @NotNull Path dirPath = parent.getPath();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Ranked along the order on screen, which is the order the tester
            // just arranged. A case already sitting in the right place keeps the
            // rank it had, so a drag writes the case that moved and leaves the
            // rest of the set alone.
            final @NotNull List<TestCaseDto> moved = TestCaseOrder.place(snapshot);

            Services.getInstance(p, ProjectIndexer.class).updateSequence(dirPath, snapshot, moved);

            // The generated methods carry the position, so a reorder has to
            // rewrite them or the run keeps executing in the order before the
            // drag. Every case in the set, not only the ones whose rank changed:
            // moving one case past three others changes where all four sit, and
            // a position is a number with no room between two of them.
            //
            // Skipped where there is nothing to write - an IDE with no Java
            // plugin answers with a no-op, and a set nobody has generated code
            // for has no methods to update.
            if (!snapshot.isEmpty()) GenType.UPDATE_TEST_CASE_ORDER.executeAll(p, snapshot);

            onPersisted.run();

            ApplicationManager.getApplication().invokeLater(this::refreshView);
        });
    }


    /**
     * UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-009.
     * <p>
     * Told, not revealed. Moving the view says nothing and changes nothing, and
     * this used to change the most visible thing the tester had set up - every
     * filter, thrown away to show one card, with no word about it. Creating a
     * test case under a filter, dragging one, and choosing a search result all
     * came through here (#205).
     * <p>
     * The test case is there either way, so this says where to look for it
     * rather than refusing anything - which is why the sentence is here and not
     * beside Testin's refusals.
     */
    @Override
    protected void notOnAnyPage(final @NotNull TestCaseDto tc) {
        Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("editor.hidden.title"),
                Bundle.message("editor.hidden.message", tc.getDescription()));
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-030
    @Override
    public void appendNewTestCase(final @NotNull TestCaseDto tc, final @NotNull Runnable onPersisted) {
        this.allTestCases.add(tc);
        orderThen(() -> {
            updateSequenceAndSaveAll(onPersisted);

            // VFS refresh goes through the indexer - file access is the
            // indexer's alone (see CLAUDE.md).
            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(parent.getPath());

            refreshView();
            selectTestCase(tc);
        });
    }

    // UC-EDITOR-PANEL-005
    @Override
    public void onToolBarCreateTestCaseClicked() {
        CreateTestCaseAction.openCreateDialog(p, this, parent);
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014
    @Override
    public @NotNull String cardTitle(final @NotNull TestCaseDto tc) {
        final @NotNull Set<TestEditorAttributes> selected = getSelectedDetails();

        return BaseCard.titleText(positionOf(tc),
                selected.contains(TestEditorAttributes.ORDER),
                selected.contains(TestEditorAttributes.DESCRIPTION) ? TestEditorAttributes.DESCRIPTION.displayValue(tc) : "");
    }

    @Override
    protected @NotNull TestEditorContextMenu buildContextMenu() {
        return new TestEditorContextMenu(p, this, parent, list, model);
    }

    @Override
    protected @NotNull Class<TestEditorAttributes> attributeType() {
        return TestEditorAttributes.class;
    }

    @Override
    public @NotNull Set<TestEditorAttributes> getSelectedDetails() {
        return getToolBar().getToolbarItem(TestDetailsPopupBtn.class).getSelectedDetails();
    }


    /**
     * The replace is not an edit, so the notifier that watches for edits is
     * quiet while it happens.
     */
    @Override
    protected void replaceModel(final @NotNull List<TestCaseDto> pageItems) {
        modelChangeNotifier.pause();
        super.replaceModel(pageItems);
        modelChangeNotifier.resume();
    }

    /**
     * UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101.
     * <p>
     * How much of this set is automated, and the message when there is nothing
     * to draw at all.
     */
    @Override
    protected void drawStatus(final @NotNull PageWindow page, final int totalItems) {
        showEmptyStateIfNothingToDraw(totalItems);

        // Fired and forgotten: the list is already on screen, and this answers
        // for it whenever it answers. Nothing waits, so a test set opens at the
        // speed it opened before; a read that fails leaves the cards drawing
        // what they drew, which is the icon the button always had.
        //
        // The whole set rather than the page, because it is one class either
        // way - and because the filter narrows the set, not the page.
        final @NotNull List<TestCaseDto> all = snapshotOfAll();
        final @NotNull AutomationState automation = Services.getInstance(p, AutomationState.class);

        automation.read(p, all, this::refreshView);

        // Read from what the service holds now, not from what the call above
        // will find: that one answers on its own time and calls back here, so
        // the count is written twice - blank on the way in, filled in when the
        // answers land.
        statusBar.showAutomated(automation.writtenIn(all), automation.knownIn(all));

        statusBar.updatePaginationState(page.page(), page.totalPages());
    }

    /**
     * UC-EDITOR-PANEL-001.
     * <p>
     * What an empty list says, decided here because this is where the page is
     * decided.
     * <p>
     * It used to be set only by the two places that load data, so a list emptied
     * any other way kept whatever message was last written - remove the last
     * test case after a refresh and the editor sat on "Refreshing..." forever,
     * for a refresh that had finished minutes ago.
     * <p>
     * Two empties, two answers: nothing in the test set at all, which is an
     * invitation to add one, and nothing matching the search, which is not - the
     * cases are there and the filter is hiding them.
     * <p>
     * Silent while loading. The load paths own that message, and overwriting it
     * here would flash "No test cases found" over data that is still on its way.
     */
    private void showEmptyStateIfNothingToDraw(final int totalItems) {
        if (totalItems > 0 || loading) return;

        if (allTestCases.isEmpty()) {
            list.getEmptyText().setText(Bundle.message("editor.test.empty")).appendLine(Bundle.message("editor.test.empty.hint"));
        } else {
            list.getEmptyText().setText(Bundle.message("editor.test.no.match"));
        }
    }


    /**
     * Re-sorts asynchronously, then persists the resulting sequence through
     * the indexer. Persisting must wait for the sort to land - callers use
     * this instead of running the two steps sequentially themselves.
     */
    public void reorderAndPersist() {
        reorderAndPersist(() -> {
        });
    }

    /**
     * The same, telling {@code onPersisted} when the new order is on disk - what
     * a paste needs before it can record what undoing itself would take.
     */
    public void reorderAndPersist(final @NotNull Runnable onPersisted) {
        orderThen(() -> updateSequenceAndSaveAll(onPersisted));
    }

    /**
     * UC-EDITOR-PANEL-009, Rule-EDITOR-PANEL-013.
     * <p>
     * Recomputes the order off the EDT (#24): the
     * walk runs on a pooled thread and the result is applied back on the EDT,
     * where onDone continues (persisting, refreshing). Any newer sort or load
     * bumps the generation, so a stale result never overwrites a newer one.
     */
    @Override
    public void refreshOrdered() {
        orderThen(this::refreshView);
    }

    private void orderThen(final @NotNull Runnable onDone) {
        final List<TestCaseDto> snapshot;
        synchronized (allTestCases) {
            snapshot = new ArrayList<>(allTestCases);
        }
        if (snapshot.isEmpty()) {
            onDone.run();
            return;
        }

        final int generation = modelGeneration.incrementAndGet();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (generation != modelGeneration.get()) return;
            final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(snapshot);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation != modelGeneration.get()) return;

                synchronized (allTestCases) {
                    this.allTestCases.clear();
                    this.allTestCases.addAll(ordered);
                }

                onDone.run();
            });
        });
    }

    /**
     * UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095.
     * <p>
     * The modules this test set uses, from {@link Modules} - which is what a
     * module is, the same way the groups below come from the owner that holds
     * those. Which cases to ask about is the editor's; what counts as a module
     * is not (#291).
     */
    @Override
    public @NotNull Set<String> getAvailableModules() {
        return Modules.in(Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(parent.getPath()));
    }

    // UC-EDITOR-PANEL-020
    @Override
    protected @NotNull JBTable buildTable(final @NotNull List<TestCaseDto> pageItems, final @NotNull Set<TestEditorAttributes> attributes) {
        return gridPanelBuilder.buildTestTable(pageItems, attributes, this::positionOf);
    }

    @Override
    protected void installEditListener(final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems) {
        table.getModel().addTableModelListener(new GridEditListener(p, pageItems, model::allContentsChanged, parent.getPath()));
    }

    @Override
    protected @NotNull List<TestCaseDto> getFilteredList() {
        final @NotNull EditorFilters filters = EditorFilters.of(toolBar);

        final @NotNull List<TestCaseDto> matched;
        synchronized (allTestCases) {
            matched = TestCaseFilter.filter(
                    allTestCases,
                    filters.query(),
                    filters.groups(),
                    filters.priorities(),
                    filters.modules());
        }

        return Services.getInstance(p, AutomationState.class).matching(matched, filters.automation());
    }

    /**
     * The test editor lets go of the notifier it put on the model; everything
     * else a tab holds is {@link AbstractTestinEditor#dispose()}.
     */
    @Override
    protected void disposeLoadedData() {
        model.removeListDataListener(modelChangeNotifier);
    }

}
