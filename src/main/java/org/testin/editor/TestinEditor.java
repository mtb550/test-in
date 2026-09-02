package org.testin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.statusbar.StatusBar;
import org.testin.logger.Logger;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.testcase.TestCaseOrder;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TestinEditor extends Disposable {

    /**
     * How many test cases a page holds until the tester says otherwise.
     * <p>
     * Declared once because it was declared three times and the three agreed by
     * luck: the run editor hard-coded fifty, the test case editor read fifty out
     * of a stored property nothing ever wrote, and the status bar's field was
     * built with the text "50" and never asked either editor what it actually
     * was. Any one of them changing would have left the field stating a number
     * the editor was not using.
     */
    int DEFAULT_PAGE_SIZE = 50;

    /**
     * The largest page a tester may ask for.
     * <p>
     * A ceiling rather than a warning, because the number is a view setting and
     * not something anyone can get wrong in a way worth interrupting them over.
     * A thousand cards is already far past what a page is for; five thousand is a
     * typing mistake, and the answer to a typing mistake is the nearest number
     * that works.
     */
    int MAX_PAGE_SIZE = 1000;

    /**
     * What a typed page size actually comes to.
     * <p>
     * Silent and total: whatever is in the field, this answers with a page size,
     * and the field is then made to show that answer. Nothing is refused and
     * nothing is announced - a tester who typed five thousand sees a thousand and
     * has learned the limit, which is what a notification would have told them at
     * the cost of a balloon on every mistyped keystroke.
     * <p>
     * Too small becomes the default rather than one, because zero, a minus sign
     * and an empty field all mean the tester did not name a size - and one case
     * per page is not what any of them were reaching for.
     * <p>
     * Digits are checked rather than parsed and caught: a key held down produces
     * more digits than a long can hold, and that is still a tester asking for a
     * very large page, which is the maximum. Catching the overflow would have
     * answered fifty.
     */
    static int pageSizeOf(final @NotNull String typed) {
        final @NotNull String asked = typed.trim();

        if (asked.isEmpty() || !asked.chars().allMatch(Character::isDigit)) return DEFAULT_PAGE_SIZE;

        final long size = asked.length() > 18 ? MAX_PAGE_SIZE : Long.parseLong(asked);

        return size < 1 ? DEFAULT_PAGE_SIZE : (int) Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * A test case is being launched from this editor.
     * <p>
     * The execution reports that follow are broadcast to every listener, and a
     * case can sit in several runs at once - so without this every open run
     * editor holding that case wrote the verdict into its own run, including
     * ones the tester was not looking at and ones already completed.
     * <p>
     * Nothing by default: the test editor draws a badge from the report and has
     * no run to record into, so only the run editor has an answer to give.
     */
    default void launching(final @NotNull java.util.UUID caseId) {
    }

    @NotNull DirectoryDto getParent();

    @NotNull StatusBar getStatusBar();

    @NotNull AbstractToolbarPanel getToolBar();

    int getCurrentPage();

    void setCurrentPage(final int page);

    int getPageSize();

    void setPageSize(final int size);

    /**
     * Whether turning this many pages would land on one that exists.
     */
    default boolean canStepPage(final int delta) {
        final int target = getCurrentPage() + delta;

        return target >= 1 && target <= getTotalPageCount();
    }

    /**
     * Turns the page by this many, and does nothing when that would leave the
     * range.
     * <p>
     * One implementation for one gesture. The status-bar arrows had their own
     * bounds check and refresh and the context-menu entries and keyboard
     * shortcuts had another, which agreed only because nobody had changed
     * either - so remembering the selection across a page turn, notifying, or
     * refusing while the editor is busy would have landed in one and not the
     * other, and the arrow would have behaved differently from the shortcut
     * with nothing failing.
     * <p>
     * First and last are this call with a computed delta, which is why there is
     * no separate method for them.
     */
    default void stepPage(final int delta) {
        if (!canStepPage(delta)) return;

        setCurrentPage(getCurrentPage() + delta);
        refreshView();
    }

    /**
     * Tells the status bar what is selected, converting the page row to its
     * place in the list being paged through on the way.
     * <p>
     * A row, deliberately, and the only sum here that still is one: the bar
     * reads "3 of 12 test cases (filtered from 120)", so its 3 counts in the
     * narrowed list the tester is looking at. The number on a card is the
     * case's place in the set instead - see {@link #positionOf}.
     * <p>
     * Here so the three callers - the selection listener and both editors after
     * a redraw - make the same call with the same conversion, rather than the
     * status bar doing the arithmetic a fourth time from two extra parameters.
     */
    default void refreshSelectionStatus(final int @NotNull [] selectedIndices) {
        final int firstRow = selectedIndices.length == 0 ? 0 : selectedIndices[0];

        getStatusBar().updateSelectionState(
                selectedIndices,
                ((getCurrentPage() - 1) * getPageSize()) + firstRow,
                getShownItemsCount(),
                getTotalItemsCount());
    }

    /**
     * The case's place in the whole test set, counting from one - the number a
     * card and a grid row show.
     * <p>
     * Not a row number. A row counts in the page of whatever the filter left, so
     * the cards renumbered themselves from one whenever a filter was on and a
     * case seventeenth in its set was drawn as third (#163). The set's own list
     * is asked instead, which is the list the ranks were written along, so the
     * number on screen is the number the generated method carries.
     * <p>
     * Default rather than repeated: both editors hold that list and neither has
     * a different answer to give.
     */
    default int positionOf(final @NotNull TestCaseDto tc) {
        final @NotNull List<TestCaseDto> all = getAllTestCases();

        synchronized (all) {
            return TestCaseOrder.positionOf(all, tc);
        }
    }

    int getTotalPageCount();

    /**
     * How many test cases the tester is paging through: the whole set, or what is
     * left of it once the filters and the search have narrowed it.
     * <p>
     * Declared beside {@link #getTotalItemsCount()} because the status bar needs
     * both and they are not the same number. It said "3 of 120 test cases" over
     * twelve visible rows, taking the position from the narrowed list and the
     * total from the whole one - two different lists in one sentence, and no way
     * for the tester to tell whether the other hundred and eight were on later
     * pages or filtered out.
     */
    int getShownItemsCount();

    int getTotalItemsCount();

    void refreshView();

    /**
     * Throws away what this editor holds and reads the index again.
     * <p>
     * {@link #refreshView()} redraws from the lists the editor is holding, which
     * is what a page change or a filter needs. This is for when those lists are
     * the wrong data altogether - a branch was checked out, a sync brought a
     * colleague's edits - and the editor has to go back to the indexer for what
     * the node holds now.
     * <p>
     * The refresh button in the editor's own toolbar is the same thing, so it
     * calls this rather than keeping a second copy of it.
     */
    void reload();

    /**
     * Reads the index again without touching the toolbar.
     * <p>
     * The difference from {@link #reload()} is the filters and the search.
     * Dropping those is what Refresh means when a tester presses the button, and
     * it is exactly wrong for anything the tester did not ask for - taking a
     * change back should not also throw away what they had narrowed the view to
     * (#165).
     */
    void reloadData();

    /**
     * Whether this editor is in the middle of something a reload would ruin - a
     * run being executed, a grid cell open under the tester's cursor.
     * <p>
     * A change noticed on disk (#20) reloads every open editor to bring it back
     * in step, but an editor busy this way holds live state the reload throws
     * away: the run's timer stops with its seconds unstamped, the half-typed cell
     * is lost. So the on-disk refresh leaves a busy editor alone and catches it up
     * next time; the tester's own Refresh button is their choice and still
     * reloads. An editor with no such state answers no.
     */
    default boolean isBusy() {
        return false;
    }

    @NotNull List<TestCaseDto> getSelectedTestCases();

    /**
     * Adds a test case the tester has just created to this editor.
     * <p>
     * Nothing by default, the same shape {@code launching} has: only the test
     * editor creates test cases, and Create Test Case is offered only there. The
     * run editor carried an implementation of this that nothing could reach -
     * dead, and wrong if anyone had wired it up, because it added to the master
     * list without ranking the case or writing it anywhere.
     * <p>
     * {@code onPersisted} is told once the case and the set's new order are both
     * on disk. A creation is ranked asynchronously - the case is saved first and
     * given its position by the sort that follows - so the caller that wants to
     * know what it would take to undo the creation cannot read it when this
     * returns.
     */
    default void appendNewTestCase(final @NotNull TestCaseDto tc, final @NotNull Runnable onPersisted) {
        Logger.debug("This editor does not create test cases; '" + tc.getDescription() + "' was not added");
    }

    @NotNull JComponent getComponent();

    @NotNull JComponent getPreferredFocusedComponent();

    @NotNull Set<?> getSelectedDetails();

    /**
     * The title line of a case's card, as it is drawn: the order number and the
     * description, each present only while its attribute is ticked in the
     * Details popup.
     * <p>
     * Lives here because the two editors hold different attribute enums, and only
     * they can read their own selection. The renderer asks for it to draw, and
     * the mouse listener asks for it to place the hover icons, so both are
     * looking at one answer.
     * <p>
     * The case rather than its row, because the number in the title is the case's
     * place in the set and only the editor can say what that is - handing a row
     * in was how a caller came to pass a filtered one (#163).
     */
    @NotNull String cardTitle(final @NotNull TestCaseDto tc);

    /**
     * Redraws what is on screen with the set's own order re-applied.
     * <p>
     * What follows an update that may have moved a case - the Order field, which
     * writes a rank and so changes where every case after it is drawn (#162). A
     * repaint alone would leave the cards in the order the list happened to be
     * in and their numbers reading wrong until the next reload.
     * <p>
     * Ordinary redraw by default: only an editor that pages through a set the
     * tester arranges has an order of its own to re-apply.
     */
    default void refreshOrdered() {
        refreshView();
    }

    @NotNull List<TestCaseDto> getAllTestCases();

    /**
     * Writes the set's order, and runs {@code onPersisted} once it is on disk.
     * <p>
     * The callback exists because the write is asynchronous and a caller that
     * has to know what the set looks like afterwards - a drag recording what it
     * would take to undo itself - cannot find out by returning.
     */
    void updateSequenceAndSaveAll(final @NotNull Runnable onPersisted);

    void selectTestCase(final @NotNull TestCaseDto tc);

    /**
     * Land on this case as soon as there is data to land on (#29).
     * <p>
     * {@link #selectTestCase} needs the editor to be holding its test cases
     * already, which an editor that was opened a moment ago is not: it reads
     * them on a pooled thread, so anything asked of it in the meantime finds an
     * empty list and quietly does nothing. That is what happened to the search -
     * picking a case in an open editor worked, and picking one in a closed
     * editor opened it on page one with nothing selected.
     * <p>
     * So this is told rather than done. The editor already remembers a case to
     * come back to across a reload, and already moves to whichever page holds it
     * before the first paint; this is the same promise, made from outside. Which
     * means the page is right too - a case that is the four hundredth of a set
     * opens on the page it is actually on.
     *
     * @param id the case, by id rather than by object: a load hands back
     *           different instances for the same cases, so the object a caller
     *           holds is not the one the editor will have
     */
    void selectWhenLoaded(final @NotNull UUID id);

    /**
     * Start this node's cases as soon as there is data to start (#36).
     * <p>
     * Told rather than done, the same promise {@link #selectWhenLoaded} makes and
     * for the same reason: an editor opened a moment ago is still reading its
     * cases on a pooled thread, so anything asked of it before that finds an
     * empty list and quietly does nothing.
     * <p>
     * Nothing by default, the shape {@code launching} and {@code appendNewTestCase}
     * already have: only a run has somewhere for the verdicts to land, so only the
     * run editor has an answer to give.
     */
    default void runWhenLoaded() {
    }

    /**
     * Which action icon the pointer is over, by name, and empty for none.
     */
    @NotNull String getHoveredIconAction();

    void setHoveredIconAction(final @NotNull String action);

    int getHoveredIndex();

    void setHoveredIndex(final int index);

    /**
     * The project this editor belongs to.
     * <p>
     * Declared because {@code dispose} needs it: the view panel is now asked
     * for by project, and an editor closing in one project must not reset
     * another project's panel.
     */
    @NotNull Project getP();

    default void dispose() {
        ViewToolWindowFactory.panel(getP()).ifPresent(ViewPanel::reset);
    }
}