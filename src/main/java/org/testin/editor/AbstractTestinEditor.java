package org.testin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.grid.GridPanelBuilder;
import org.testin.editor.grid.GridView;
import org.testin.editor.list.ListPanelBuilder;
import org.testin.editor.list.ListView;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.Toolbar;
import com.intellij.ui.table.JBTable;
import org.testin.actions.EscapeAction;
import org.testin.editor.grid.GridEnterAction;
import org.testin.editor.listeners.GridContextMenuListener;
import org.testin.editor.listeners.GridSelectionListener;
import org.testin.editor.statusbar.PageAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.ui.FontSync;
import org.testin.actions.Declared;
import org.testin.logger.Logger;
import org.testin.model.ToolBarAttribute;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.awt.event.MouseListener;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What a test editor and a run editor are the same thing about: a page of test
 * cards, a grid of the same cases, a toolbar above them and a status bar below,
 * and the paging, filtering and selection that connect the four.
 * <p>
 * The two editors were written one after the other and shared 37 method
 * signatures, fifteen of them line for line. Of the rest, most differed only in
 * which attribute enum they named - {@code rebuildGrid}, {@code cardTitle},
 * {@code updateGridColumns}, the toolbar callbacks - so a paging fix had two
 * places to be made and a grid fix two places to be forgotten (#109).
 * <p>
 * <b>Typed by its attributes.</b> {@code A} is the enum whose constants the
 * Details popup lists and the grid draws columns for: {@code
 * TestEditorAttributes} for a test set, {@code RunEditorAttributes} for a run.
 * {@code N} is the node being edited. A third editor supplies two type
 * arguments and the handful of methods below that are abstract.
 * <p>
 * <b>What is not here</b> is what actually differs: where the cases come from,
 * what a card's title says, and - the run editor's alone - the walk through a
 * run and the verdicts it records.
 */
public abstract class AbstractTestinEditor<A extends Enum<A> & ToolBarAttribute, N extends DirectoryDto> implements Disposable, Toolbar, TestinEditor {

    @Getter
    protected final @NotNull Project p;

    @Getter
    protected final @NotNull N parent;

    @Getter
    protected final @NotNull List<TestCaseDto> allTestCases;

    @Getter
    protected final @NotNull List<TestCaseDto> currentTestCases;

    protected final @NotNull GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    protected final @NotNull Disposable projectDisposable;
    protected final @NotNull JBPanel<?> mainPanel;
    protected final @NotNull EditorCenter center;

    @Getter
    protected final @NotNull JBList<TestCaseDto> list;

    protected final @NotNull CollectionListModel<TestCaseDto> model;
    protected final @NotNull JBScrollPane scrollPane;

    /**
     * The three of them together, which is what the shared wiring is handed.
     */
    protected final @NotNull ListView listView;

    /**
     * The menu this editor's list and grid answer with. The subclass says which
     * menu; when it is built is not its business, so the field is final and
     * nothing can be wired to a menu that is not there yet.
     */
    protected final @NotNull AbstractEditorContextMenu contextMenu;

    /**
     * The grid, from the moment the tester first switches to it - table, scroll
     * pane and the font-sync subscription that goes with them. Empty until then,
     * and after a rebuild that failed (#66, finding 18).
     */
    protected @NotNull Optional<GridView> grid = Optional.empty();

    @Getter
    @Setter
    protected int currentPage = 1;

    @Getter
    @Setter
    protected int pageSize = TestinEditor.DEFAULT_PAGE_SIZE;

    /**
     * Assigned by the subclass, because each editor has its own toolbar and its
     * own way of laying the panel out around it.
     */
    @Getter
    protected @NotNull AbstractToolbarPanel toolBar;

    @Getter
    protected @NotNull StatusBar statusBar;

    @Getter
    @Setter
    protected @NotNull String hoveredIconAction = "";

    @Getter
    @Setter
    protected int hoveredIndex = -1;

    /**
     * Test case selected before a reload. Held as an id, not as a dto: a reload
     * hands back different objects for the same test cases and TestCaseDto has
     * no equals, so identity would not survive it.
     */
    protected @NotNull Optional<UUID> selectionToRestore = Optional.empty();

    /**
     * Grid column selected before a reload, so the cell comes back, not just the
     * row.
     */
    protected int gridColumnToRestore = -1;

    /**
     * True when the selection being restored is one a tester asked to be taken
     * to, rather than one a reload is putting back - the difference being which
     * of the two takes the focus.
     */
    private boolean goingTo = false;

    protected AbstractTestinEditor(final @NotNull Project p, final @NotNull N parent) {
        this.p = p;
        this.parent = parent;

        final @NotNull Disposable projectDisposable = Disposer.newDisposable();
        Disposer.register(p, projectDisposable);
        this.projectDisposable = projectDisposable;

        this.allTestCases = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCases = Collections.synchronizedList(new ArrayList<>());

        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.center = new EditorCenter(this.mainPanel);
        this.mainPanel.setBackground(UIUtil.getPanelBackground());
        this.mainPanel.setOpaque(true);

        // Shared list-view construction (see ListPanelBuilder, the counterpart
        // of GridPanelBuilder). Built here rather than by the subclass so the
        // three parts of it are final: an editor never exists without a list.
        this.listView = ListPanelBuilder.build(p, projectDisposable, this);
        this.model = listView.model();
        this.list = listView.list();
        this.scrollPane = listView.scrollPane();

        this.contextMenu = buildContextMenu();
    }

    /**
     * The menu for this editor's cases. Built from the list and the model above
     * it, which is all either implementation reads - so it is safe to ask the
     * subclass for it before the subclass constructor has run.
     */
    protected abstract @NotNull AbstractEditorContextMenu buildContextMenu();

    /**
     * The enum whose constants this editor's Details popup lists, so the grid
     * can be asked which of its columns to show. An enum class rather than its
     * values, because the grid needs the ones that are <i>not</i> selected too.
     */
    protected abstract @NotNull Class<A> attributeType();

    /**
     * The attributes ticked in the Details popup right now.
     */
    @Override
    public abstract @NotNull Set<A> getSelectedDetails();

    /**
     * The cases that pass the toolbar's search and filters, in the order they
     * are shown. What a filter is differs: a run has a verdict to filter on and
     * a test set has not.
     */
    protected abstract @NotNull List<TestCaseDto> getFilteredList();

    /**
     * Reads the cases this editor shows and puts them on screen, telling {@code
     * onLoaded} once they are there - which is the only moment a refresh can
     * honestly be confirmed.
     */
    protected abstract void loadDataAsync(final @NotNull Runnable onLoaded);

    // ---------------------------------------------------------------- the panel

    /**
     * Reported as not reachable from the entry points, and kept: this and
     * {@link #getPreferredFocusedComponent()} are the {@code FileEditor}
     * contract. The platform calls them when it shows the tab; no code in the
     * plugin does, and no inspection can see that call (#61).
     */
    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    public @NotNull JComponent getPreferredFocusedComponent() {
        return list;
    }

    @Override
    public @NotNull Project getProject() {
        return p;
    }

    /**
     * This editor's own node, for the toolbar's Details button. The field is
     * called parent because the test cases are its children; what the toolbar
     * wants is the node itself, so it is named for that.
     */
    @Override
    public @NotNull DirectoryDto getEditedNode() {
        return parent;
    }

    // ------------------------------------------------------------ what is shown

    @Override
    public int getShownItemsCount() {
        return currentTestCases.size();
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCases.size();
    }

    @Override
    public int getTotalPageCount() {
        return getTotalPages(currentTestCases);
    }

    protected int getTotalPages(final @NotNull List<TestCaseDto> filtered) {
        return PageWindow.of(filtered.size(), currentPage, pageSize).totalPages();
    }

    protected @NotNull List<TestCaseDto> getCurrentPageItems() {
        final @NotNull PageWindow page = PageWindow.of(currentTestCases.size(), currentPage, pageSize);

        // Copied rather than a subList view: listeners retain this list, and a
        // live view would throw ConcurrentModificationException the next time
        // currentTestCases is touched.
        return new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));
    }

    @Override
    public @NotNull List<TestCaseDto> getSelectedTestCases() {
        return list.getSelectedValuesList();
    }

    /**
     * UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095.
     * <p>
     * Every group the project has used, from the cache that already holds them
     * for the completion field (#296).
     */
    @Override
    public @NotNull Set<String> getAvailableGroups() {
        return Services.getInstance(p, TestCaseValues.class).getGroups();
    }

    /**
     * UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119.
     * <p>
     * Busy while a grid cell is open for editing - a reload would discard the
     * half-typed value, so an on-disk refresh leaves the editor be until the
     * tester is done (#20).
     */
    @Override
    public boolean isBusy() {
        return grid.map(GridView::isCellOpen).orElse(false);
    }

    /**
     * Re-measures the cards. A model event rather than a repaint: a card that
     * grows a line is a taller row, and JList re-measures a row only when the
     * model says that row changed.
     */
    protected void refreshCards() {
        model.allContentsChanged();
    }

    // ------------------------------------------------------------- the tool bar

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-092
    @Override
    public void onToolBarSearchValueChanged() {
        currentPage = 1;
        refreshView();
    }

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-093
    @Override
    public void onToolBarSearchFocusReleased() {
        list.requestFocusInWindow();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-097
    @Override
    public void onToolBarFilterSelectionChanged() {
        currentPage = 1;
        refreshView();
    }

    // UC-EDITOR-PANEL-021, Rule-EDITOR-PANEL-099
    @Override
    public void onToolBarFilterResetButtonClicked() {
        currentPage = 1;
        refreshView();
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-021
    @Override
    public void onToolBarDetailsSelectionChanged() {
        Logger.debug("[details] selectedDetails changed -> " + getSelectedDetails());

        // Only what is on screen. Re-measuring the cards costs a full pass over
        // the page, and doing it while the grid is showing buys nothing - the
        // list is re-measured when it comes back instead.
        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[details] grid active -> toggling column visibility");
            updateGridColumns();
        } else {
            refreshCards();
        }
    }

    protected void updateGridColumns() {
        grid.ifPresent(view -> gridPanelBuilder.applyColumnVisibility(view.table(), attributeType(), getSelectedDetails()));
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-018
    @Override
    public void onToolBarSwitchedToListView() {
        Logger.debug("[switch] -> LIST view, currentView=" + toolBar.getCurrentView());
        center.set(scrollPane);

        // Attributes ticked while the grid was showing did not touch the cards;
        // they are re-measured here, once, rather than on every tick.
        refreshCards();
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-017
    @Override
    public void onToolBarSwitchedToGridView() {
        Logger.debug("[switch] -> GRID view, currentView=" + toolBar.getCurrentView());
        rebuildGrid();
        // rebuildGrid() swallows failures; the grid is then still empty and the
        // previous center stays visible instead of an NPE.
        grid.ifPresent(view -> {
            center.set(view.scrollPane());
            ApplicationManager.getApplication().invokeLater(view.table()::requestFocusInWindow);
        });
    }

    /**
     * The grid of this page, built out of the attributes ticked in the Details
     * popup - a different table for a run, which draws what it recorded.
     */
    protected abstract @NotNull JBTable buildTable(final @NotNull List<TestCaseDto> pageItems, final @NotNull Set<A> attributes);

    /**
     * What typing into a cell does. Editing a test case writes the case; editing
     * a run item writes the run (#74).
     */
    protected abstract void installEditListener(final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems);

    /**
     * UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-017.
     * <p>
     * Builds the grid again from the page as it stands, and gives it everything
     * the list has: the selection, the keys, the context menu and the page
     * shortcuts.
     * <p>
     * Failures are swallowed deliberately - the grid is then still whatever it
     * was, and the previous center stays visible instead of an exception on the
     * EDT.
     */
    protected void rebuildGrid() {
        // Before the page is read, not after: the committed value has to be in the
        // data the new grid is built from, or the tester watches their own
        // sentence disappear and come back on the next refresh.
        final boolean keepKeyboard = grid.map(GridView::handOver).orElse(false);

        final @NotNull List<TestCaseDto> pageItems = getCurrentPageItems();
        final @NotNull Set<A> attributes = getSelectedDetails();
        Logger.debug("[grid] rebuildGrid start, pageItems=" + pageItems.size() + ", details=" + attributes);
        try {
            final @NotNull JBTable table = buildTable(pageItems, attributes);

            // The previous grid's subscription goes with the previous grid, so
            // they do not accumulate one per rebuild.
            grid.ifPresent(previous -> Disposer.dispose(previous.fontSync()));
            final @NotNull Disposable fontSync = Disposer.newDisposable(projectDisposable, "testin." + getClass().getSimpleName() + ".gridFontSync");
            FontSync.syncWithNativeEditor(p, table, fontSync);

            table.getSelectionModel().addListSelectionListener(new GridSelectionListener(this, table, list, pageItems));
            installEditListener(table, pageItems);
            // ESC in grid view behaves like ESC in the list: hide the view panel, then clear the selection.
            new EscapeAction(p, table);
            // Everything ENTER does in this grid, and the double click on the sequence.
            new GridEnterAction(p, table, pageItems, parent.getPath2());
            table.addMouseListener(new GridContextMenuListener(table, list, contextMenu, pageItems));
            // Every shortcut the menu offers - the verdict keys above all - live
            // on the grid too, and quiet while a cell is open (#74).
            contextMenu.bindShortcutsTo(table);
            // And the page keys, which are not on the menu and so are not
            // carried across by the line above.
            PageAction.bindToGrid(this, table);
            new OpenContextMenuAction(table, contextMenu);

            grid = Optional.of(GridPanelBuilder.finishRebuild(table, list, pageItems, gridColumnToRestore, fontSync, keepKeyboard));

            // Cleared regardless of whether the row was found, so a stale column can
            // never be applied to an unrelated rebuild.
            gridColumnToRestore = -1;
            Logger.debug("[grid] rebuildGrid done, rows=" + table.getRowCount() + ", cols=" + table.getColumnCount());
        } catch (final Exception ex) {
            Logger.error("[grid] rebuildGrid FAILED: " + ex);
        }
    }

    // ------------------------------------------------------------- loading data

    protected void loadDataAsync() {
        loadDataAsync(() -> {
        });
    }

    @Override
    public void reloadData() {
        reloadData(() -> {
        });
    }

    /**
     * Throws away what is on screen and reads it again, telling {@code onLoaded}
     * when it is back.
     */
    protected void reloadData(final @NotNull Runnable onLoaded) {
        beforeReload();

        rememberSelection();

        allTestCases.clear();
        currentTestCases.clear();
        clearLoadedData();

        model.removeAll();
        list.setPaintBusy(true);
        list.getEmptyText().setText(Bundle.message("editor.refreshing"));

        loadDataAsync(onLoaded);
    }

    /**
     * Before anything is thrown away. Nothing for a test set; the run editor
     * stops the walk, because the copy it is walking is about to be replaced.
     */
    protected void beforeReload() {
    }

    /**
     * Whatever else the subclass holds about the cases it is about to re-read.
     */
    protected void clearLoadedData() {
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-117
    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + toolBar.getCurrentView());

        // Asked before the reload, because the reload is what makes the answer
        // stale: a run that was executing is not executing once it has been
        // read again, and the message has to carry what it was (#218).
        final @NotNull Done message = refreshed();

        // Said when the data is back, not when the button went down: the read
        // waits for indexing and finishes on another thread, so a balloon here
        // would announce a refresh that has not happened yet (#62).
        reloadData(() -> Services.getInstance(p, Notifier.class).softShow(p, message));
    }

    /**
     * What the balloon says once a refresh has landed. Asked before the reload
     * rather than after, because the reload is what makes the answer stale.
     */
    protected @NotNull Done refreshed() {
        return Done.REFRESHED;
    }

    // -------------------------------------------------------------- selection

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-104
    @Override
    public void selectWhenLoaded(final @NotNull UUID id) {
        final @NotNull Optional<TestCaseDto> loaded = currentTestCases.stream()
                .filter(tc -> id.equals(tc.getId()))
                .findFirst();

        // Already holding it, so nothing is coming to do this later: go now,
        // through the one method that owns going somewhere - it turns to the
        // right page and takes the focus with it.
        if (loaded.isPresent()) {
            selectTestCase(loaded.get());
            return;
        }

        // Not loaded yet. The load ends by moving to the page that holds the
        // remembered case and repainting, which is exactly what is wanted -
        // all that is missing is the focus, because this is a tester asking to
        // be taken somewhere rather than a reload putting things back.
        selectionToRestore = Optional.of(id);
        goingTo = true;
    }

    /**
     * Focuses the list when the case that has just been restored is one the
     * tester asked to be taken to. Called once the page and the selection are
     * settled, because focusing a row that is about to be replaced is no use.
     */
    protected void focusIfGoingTo() {
        if (!goingTo) return;

        goingTo = false;
        list.requestFocusInWindow();
    }

    /**
     * UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-009.
     * <p>
     * Puts the tester on this case: the page it is on, the row, and the focus.
     * <p>
     * Both editors worked the page out for themselves, from the same two fields,
     * in methods that had drifted - one bounds-checked the row and one did not
     * (#66, finding 31). What differs is only what to do about a case that is
     * not on any page, which is {@link #notOnAnyPage}.
     */
    @Override
    public void selectTestCase(final @NotNull TestCaseDto tc) {
        final int index = currentTestCases.indexOf(tc);
        if (index < 0) {
            notOnAnyPage(tc);
            return;
        }

        final int safePageSize = Math.max(1, pageSize);
        final int page = (index / safePageSize) + 1;
        final int localIndex = index % safePageSize;

        if (page == currentPage) {
            selectVisibleIndex(localIndex);
            return;
        }

        currentPage = page;
        refreshView();

        // After the redraw, because the row does not exist until the page it is
        // on has been drawn.
        ApplicationManager.getApplication().invokeLater(() -> selectVisibleIndex(localIndex));
    }

    /**
     * The case the tester was sent to is not on any page - filtered out, or
     * searched out. Nothing by default; the test editor says where to look.
     */
    protected void notOnAnyPage(final @NotNull TestCaseDto tc) {
    }

    /**
     * The row on the page now showing, selected, scrolled to and focused.
     * <p>
     * Bounds-checked because a redraw can land between the page being chosen and
     * this running, and a row that is no longer there is not an error - it is a
     * tester who did something else in the meantime.
     */
    protected void selectVisibleIndex(final int index) {
        if (index < 0 || index >= list.getModel().getSize()) return;

        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
        list.requestFocusInWindow();
    }

    /**
     * The case to put the selection back on after a reload. Swing answers null
     * when nothing is selected, which is the one thing there is nothing to
     * remember about.
     */
    protected void rememberSelection() {
        selectionToRestore = Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId);
    }

    /**
     * The keys and listeners a card list answers, once the subclass has its
     * toolbar and its context menu.
     * <p>
     * Three of the four keys are bound on the declared action rather than
     * entered in the keymap: ENTER on a list is that list's gesture rather than
     * a command, and CTRL+C and DELETE are answered by the grid and the tree for
     * their own selections - a keymap entry would answer for all of them (#119).
     * Paging is the editor's own key, so the editor binds it rather than the
     * menu.
     */
    protected void wireList() {
        PageAction.bindTo(this, list);
        Declared.bindTo("Testin.ViewDetails", list);
        Declared.bindTo("Testin.CopyTestCase", list);
        Declared.bindTo("Testin.RemoveTestCase", list);

        ListPanelBuilder.wireCommonListeners(p, this, listView, parent, contextMenu,
                () -> grid.map(GridView::table),
                () -> toolBar.getCurrentView() == ViewMode.GRID_VIEW);
    }

    // ------------------------------------------------------------------ drawing

    /**
     * UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101.
     * <p>
     * Draws the page: which cases pass the filter, which of them this page
     * holds, and where the selection lands among them.
     * <p>
     * <b>Recomputed here rather than by the callers.</b> The view is a filtered
     * page of the master list, so anything that changes that list - deleting,
     * pasting, reordering, recording a verdict - has to show at once. Leaving it
     * to whoever changed the data means the one caller that forgets leaves a
     * deleted test case on screen until the next explicit refresh; four callers
     * in the run editor hand-copied these two lines before every call, which was
     * four chances to be the one that forgets.
     * <p>
     * The three hooks are where the two editors differ: what else goes in the
     * status bar, what is drawn once the selection is back, and what a redraw
     * means to a run that is being walked.
     */
    @Override
    public void refreshView() {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());

        final int totalItems = currentTestCases.size();
        final @NotNull PageWindow page = PageWindow.of(totalItems, currentPage, pageSize);
        currentPage = page.page();

        // Copied rather than a subList view: listeners retain this list, and a
        // live view would throw ConcurrentModificationException after
        // currentTestCases is next mutated.
        final @NotNull List<TestCaseDto> pageItems = new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));

        // What was selected before the reload, or what is selected right now.
        // Swing answers null for an empty selection, which is converted here.
        final @NotNull Optional<UUID> selectedId = selectionToRestore
                .or(() -> Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId));

        replaceModel(pageItems);

        // Matched by id: a reload hands back different dto instances for the
        // same test cases, so comparing objects would drop the selection.
        selectedId.ifPresent(id -> {
            for (final TestCaseDto item : pageItems) {
                if (id.equals(item.getId())) {
                    // Selected by value, not by index: the list model owns its own
                    // ordering, so an index into pageItems is not safe to reuse.
                    list.setSelectedValue(item, true);
                    break;
                }
            }
        });
        selectionToRestore = Optional.empty();

        drawStatus(page, totalItems);

        // After the selection has been restored above, which is the whole point:
        // the label says what is selected, so it cannot be written before that
        // is known.
        refreshSelectionStatus(list.getSelectedIndices());
        afterSelectionShown();

        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            grid.ifPresent(view -> center.set(view.scrollPane()));
        }

        afterViewRefreshed();
    }

    /**
     * Puts the page into the list model. The test editor quiets the notifier it
     * put on that model while it happens, because a replace is not an edit.
     */
    protected void replaceModel(final @NotNull List<TestCaseDto> pageItems) {
        model.replaceAll(pageItems);
    }

    /**
     * What this editor writes into the status bar on every draw, beside the page
     * count - and for the test editor, the empty-state message.
     */
    protected abstract void drawStatus(final @NotNull PageWindow page, final int totalItems);

    /**
     * Drawn once the selection is back and the label beside it is written.
     */
    protected void afterSelectionShown() {
    }

    /**
     * The last word on a redraw. A run tells its buttons: what there is to walk
     * is this list, so a filter that empties it grays Start on the same redraw
     * rather than at whatever happens next (Rule-EDITOR-PANEL-135).
     */
    protected void afterViewRefreshed() {
    }

    // ---------------------------------------------------------------- closing

    /**
     * Everything an editor holds that outlives a tab if nobody lets go of it:
     * the font-sync subscriptions, the list's mouse listeners, the toolbar, the
     * model and the cases themselves.
     * <p>
     * The two hooks are what each editor holds of its own - a run editor stops
     * the walk it was on and writes the run before any of this happens.
     * <p>
     * <b>Steps rather than statements</b>, because a tab can close while the
     * project is closing and the two that ask the service container throw
     * exactly then. As one run of statements the first throw skipped every line
     * after it - the TestNG process this editor launched left running, its
     * verdicts written into no run, the subscriptions never released and a live
     * Swing timer never stopped (#66, finding 70).
     */
    @Override
    public void dispose() {
        teardown(
                this::beforeDispose,

                // Releases the message-bus subscriptions (font sync) registered
                // against this editor's lifetime.
                () -> Disposer.dispose(projectDisposable),

                this::stopListening,
                toolBar::dispose,

                // The tab is closing, so what CTRL+Z could take back in it
                // closes with it - and the copies those operations were holding
                // aside are released rather than kept for the life of the
                // project (#66, finding 45).
                () -> Services.getInstance(p, UndoHistories.class).forget(UndoScope.of(parent.getPath())),

                allTestCases::clear,
                currentTestCases::clear,
                this::disposeLoadedData,

                model::removeAll,
                mainPanel::removeAll,

                TestinEditor.super::dispose);

        Logger.debug("dispose " + getClass().getSimpleName() + ": " + parent.getName() + " - " + parent.getPath());
    }

    /**
     * Runs every step, whatever the one before it did.
     * <p>
     * A teardown is a list of independent releases, and the caller that started
     * it is the platform closing a tab - there is nobody above to retry or to
     * report to, so a step that fails is written down and the next one runs. The
     * alternative is the one this replaced: the first failure keeps everything
     * after it, which is the opposite of what a dispose is for.
     * <p>
     * Protected because the run editor tears down in steps of its own, for the
     * same reason (#66, finding 70).
     */
    protected final void teardown(final @NotNull Runnable... steps) {
        for (final Runnable step : steps) {
            try {
                step.run();
            } catch (final Exception ex) {
                Logger.warn("Teardown step failed in " + getClass().getSimpleName() + ", the rest still ran: " + ex);
            }
        }
    }

    /**
     * The listeners this editor put on the list, taken off again.
     */
    private void stopListening() {
        for (final MouseListener listener : list.getMouseListeners())
            list.removeMouseListener(listener);
    }

    /**
     * Before anything is torn down.
     */
    protected void beforeDispose() {
    }

    /**
     * Whatever else the subclass holds about the cases, let go of before the
     * model is emptied.
     */
    protected void disposeLoadedData() {
    }

    /**
     * UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-118.
     * <p>
     * Moves to whichever page now holds the remembered test case.
     */
    protected void jumpToPageOfPendingSelection() {
        final int page = selectionToRestore
                .map(id -> PageWindow.pageContaining(id, currentTestCases, pageSize))
                .orElse(0);

        // Not on any page anymore - the case was deleted or filtered out, so
        // there is nothing left to restore.
        if (page == 0) selectionToRestore = Optional.empty();
        else currentPage = page;
    }
}
