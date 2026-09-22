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

package org.testin.editor.test;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.AutomationState;
import org.testin.codegen.GenType;
import org.testin.editor.AbstractTestinEditor;
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
import org.testin.editor.toolbar.TestToolbar;
import org.testin.editor.toolbar.Toolbar;
import org.testin.editor.toolbar.components.TestDetailsPopupBtn;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.Modules;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseAction;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestEditorAttributes;
import org.testin.util.Bundle;

import javax.swing.DropMode;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TestEditor extends AbstractTestinEditor<TestEditorAttributes, TestSetDirectoryDto> implements Toolbar {
    private final @NotNull ModelChangeNotifier modelChangeNotifier;

    @Getter
    private final @NotNull TestToolbar toolBar;
    private final @NotNull AtomicInteger modelGeneration = new AtomicInteger();

    private volatile boolean loading;

    public TestEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        super(p, vf.getTestSet());

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

        mainPanel.add(statusBar, BorderLayout.SOUTH);
        StatusBarListener.attach(this);

        TestCaseExecutionSubscriber.onReported(p, projectDisposable, (_, _, _, _) -> list.repaint());

        onToolBarSwitchedToListView();

        loadDataAsync();
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-119
    @Override
    public boolean isLoading() {
        return loading;
    }

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

                    jumpToPageOfPendingSelection();

                    list.setPaintBusy(false);
                    loading = false;

                    refreshView();
                    focusIfGoingTo();
                    onLoaded.run();
                });

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
            try {
                final @NotNull List<TestCaseDto> moved = TestCaseOrder.place(snapshot);

                Services.getInstance(p, ProjectIndexer.class).updateSequence(dirPath, snapshot, moved);

                if (!snapshot.isEmpty()) GenType.UPDATE_TEST_CASE_ORDER.executeAll(p, snapshot);

                onPersisted.run();

                ApplicationManager.getApplication().invokeLater(this::refreshView);

            } catch (final Exception ex) {
                Logger.error("Failed to save the test case sequence: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("save.failed"));
                    loadDataAsync();
                });
            }
        });
    }

    // UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-009
    @Override
    protected void notOnAnyPage(final @NotNull TestCaseDto tc) {
        Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("editor.hidden.title"),
                Bundle.message("editor.hidden.message", tc.getDescription()));
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-030
    @Override
    public void appendNewTestCase(final @NotNull TestCaseDto tc, final @NotNull Runnable onPersisted) {
        hold(tc);
        orderThen(() -> {
            hold(tc);

            updateSequenceAndSaveAll(onPersisted);

            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(parent.getPath());

            refreshView();
            selectTestCase(tc);
        });
    }

    private void hold(final @NotNull TestCaseDto tc) {
        synchronized (allTestCases) {
            if (allTestCases.stream().noneMatch(held -> held.getId().equals(tc.getId()))) allTestCases.add(tc);
        }
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-119
    @Override
    public void onToolBarCreateTestCaseClicked() {
        if (loading) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("create.case.still.loading"));
            return;
        }

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
        return new TestEditorContextMenu(p, this, parent, list);
    }

    @Override
    protected @NotNull Class<TestEditorAttributes> attributeType() {
        return TestEditorAttributes.class;
    }

    @Override
    public @NotNull Set<TestEditorAttributes> getSelectedDetails() {
        return getToolBar().getToolbarItem(TestDetailsPopupBtn.class).getSelectedDetails();
    }

    @Override
    protected void replaceModel(final @NotNull List<TestCaseDto> pageItems) {
        modelChangeNotifier.pause();
        super.replaceModel(pageItems);
        modelChangeNotifier.resume();
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101
    @Override
    protected void drawStatus(final @NotNull PageWindow page, final int totalItems) {
        showEmptyStateIfNothingToDraw(totalItems);

        final @NotNull List<TestCaseDto> all = snapshotOfAll();
        final @NotNull AutomationState automation = Services.getInstance(p, AutomationState.class);

        automation.read(p, all, this::refreshView);

        statusBar.showAutomated(automation.writtenIn(all), automation.knownIn(all));

        statusBar.updatePaginationState(page.page(), page.totalPages());
    }

    // UC-EDITOR-PANEL-001
    private void showEmptyStateIfNothingToDraw(final int totalItems) {
        if (totalItems > 0 || loading) return;

        if (allTestCases.isEmpty()) {
            list.getEmptyText().setText(Bundle.message("editor.test.empty")).appendLine(Bundle.message("editor.test.empty.hint"));
        } else {
            list.getEmptyText().setText(Bundle.message("editor.test.no.match"));
        }
    }

    public void reorderAndPersist() {
        reorderAndPersist(() -> {
        });
    }

    public void reorderAndPersist(final @NotNull Runnable onPersisted) {
        orderThen(() -> updateSequenceAndSaveAll(onPersisted));
    }

    // UC-EDITOR-PANEL-009, Rule-EDITOR-PANEL-013
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
            final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(snapshot);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation == modelGeneration.get()) {
                    synchronized (allTestCases) {
                        this.allTestCases.clear();
                        this.allTestCases.addAll(ordered);
                    }
                }

                onDone.run();
            });
        });
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
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

    @Override
    protected void disposeLoadedData() {
        model.removeListDataListener(modelChangeNotifier);
    }
}
