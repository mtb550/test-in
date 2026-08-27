package org.testin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TestinEditor extends Disposable {

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
     * The row's position in the whole list rather than on the page it is drawn
     * on. The number the card shows, and what every lookup by index outside the
     * page needs.
     * <p>
     * Default rather than repeated: the renderer, the hover hit-test and the
     * transfer handler all convert the same way, and a card that numbered rows
     * differently from the handler acting on them would be a quiet mismatch.
     */
    default int globalIndex(final int rowIndex) {
        return ((getCurrentPage() - 1) * getPageSize()) + rowIndex;
    }

    int getTotalPageCount();

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

    void appendNewTestCase(final @NotNull TestCaseDto tc);

    @NotNull JComponent getComponent();

    @NotNull JComponent getPreferredFocusedComponent();

    @NotNull Set<?> getSelectedDetails();

    /**
     * The title line of the card at this row, as it is drawn: the order number
     * and the description, each present only while its attribute is ticked in
     * the Details popup.
     * <p>
     * Lives here because the two editors hold different attribute enums, and only
     * they can read their own selection. The renderer asks for it to draw, and
     * the mouse listener asks for it to place the hover icons, so both are
     * looking at one answer.
     */
    @NotNull String cardTitle(final int globalIndex, final @NotNull TestCaseDto tc);

    @NotNull List<TestCaseDto> getAllTestCases();

    void updateSequenceAndSaveAll();

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