package org.testin.editor;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.*;
import java.util.List;
import java.util.Set;

public interface TestinEditor extends Disposable {
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

    @NotNull List<TestCaseDto> getSelectedTestCases();

    void appendNewTestCase(final @NotNull TestCaseDto tc);

    @NotNull JComponent getComponent();

    @Nullable JComponent getPreferredFocusedComponent();

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

    void selectTestCase(final @Nullable TestCaseDto tc);

    /**
     * Which action icon the pointer is over, by name, and empty for none.
     */
    @NotNull String getHoveredIconAction();

    void setHoveredIconAction(final @NotNull String action);

    int getHoveredIndex();

    void setHoveredIndex(final int index);

    default void dispose() {
        ViewToolWindowFactory.panel().ifPresent(ViewPanel::reset);
    }
}