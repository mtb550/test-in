package org.testin.editor.statusbar;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.model.TestRunStatus;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

public class StatusBar extends JBPanel<StatusBar> {

    /**
     * How far the content sits from either end of the bar.
     * <p>
     * On the bar itself so the two ends cannot differ. They were a left border on
     * the sentence and a right border on the row of figures, two numbers that had
     * to be kept equal by hand and were not - and changing one of them was a
     * change to one side of the bar, which is never what anybody means.
     */
    private static final int EDGE = 2;

    private final @NotNull JBLabel statusLabel = new JBLabel();

    /**
     * Where the run is in its lifecycle, with the icon the project tree draws for
     * the same node. Only the run editor writes it; in the test case editor it
     * stays blank, which is what a label with nothing to say looks like.
     */
    private final @NotNull JBLabel runStatusLabel = new JBLabel();

    /**
     * How the run is going, in the same four verdicts the reports and the Result
     * Analysis dialog use. Blank in the test case editor, for the reason above.
     */
    private final @NotNull JBLabel verdictsLabel = new JBLabel();

    /**
     * The run's total execution time, ticking while a case is timed. Blank in the
     * test case editor, for the reason above.
     */
    private final @NotNull JBLabel executionTimeLabel = new JBLabel();

    @Getter
    private final @NotNull PageBtn firstButton = new PageBtn("First page", AllIcons.Actions.Play_first);

    @Getter
    private final @NotNull PageBtn prevButton = new PageBtn("Previous page", AllIcons.Actions.Play_back, Shortcuts.PreviousTestCase);

    private final @NotNull JBLabel currentPageLabel = new JBLabel("1 of 1");

    @Getter
    /**
     * Empty until the editor says what its page size is - see
     * {@code StatusBarListener}. The four columns are for width, not content - a
     * field built with the number in it was a fourth place claiming to know it -
     * and four because {@link TestinEditor#MAX_PAGE_SIZE} is four digits, so the
     * largest page anyone can ask for fits without the field growing to show it.
     */
    private final @NotNull JBTextField pageSizeField = new JBTextField("", 4);

    @Getter
    private final @NotNull PageBtn nextButton = new PageBtn("Next page", AllIcons.Actions.Play_forward, Shortcuts.NextTestCase);

    @Getter
    private final @NotNull PageBtn lastButton = new PageBtn("Last page", AllIcons.Actions.Play_last);

    /** The three regions, held because {@link #doLayout()} places them itself. */
    private final @NotNull JBPanel<?> navigationRow;
    private final @NotNull JBPanel<?> rightRow;

    public StatusBar() {
        super(null);

        setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
                JBUI.Borders.empty(0, EDGE)
        ));
        setBackground(JBUI.CurrentTheme.EditorTabs.background());
        statusLabel.setForeground(UIUtil.getContextHelpForeground());
        // A label reports its preferred size as its minimum, which would make this
        // sentence decide how narrow the editor may be - so it is told it can give
        // room up. Not all of it: a minimum of zero let the layout take the whole
        // label away on a tight bar, and a sentence that vanishes is worse than
        // one cut short, because nothing is left to say it was ever there. This
        // is about the width of "12 of 340 test cases", so what survives is the
        // numbers, and the ellipsis falls on the words after them.

        runStatusLabel.setForeground(UIUtil.getContextHelpForeground());
        runStatusLabel.setBorder(JBUI.Borders.emptyRight(10));
        runStatusLabel.setIconTextGap(JBUI.scale(4));
        new HelpTooltip()
                .setDescription(HtmlChunk.text("This run's status. A completed or closed run records no more verdicts"))
                .installOn(runStatusLabel);

        verdictsLabel.setForeground(UIUtil.getInactiveTextColor());
        // Second in line to give room up, after the sentence on the left has
        // given all it can. Longest of the figures and the one that survives
        // being cut best: it reads left to right in order of what a tester wants
        // to know first, so what a narrow bar loses is the untouched count.
        verdictsLabel.setBorder(JBUI.Borders.emptyRight(10));
        new HelpTooltip()
                .setDescription(HtmlChunk.text("How this run is going"))
                .installOn(verdictsLabel);

        executionTimeLabel.setForeground(UIUtil.getInactiveTextColor());
        executionTimeLabel.setBorder(JBUI.Borders.emptyRight(10));
        new HelpTooltip()
                .setDescription(HtmlChunk.text("Time spent executing this run"))
                .installOn(executionTimeLabel);

        // The three above start hidden, and each shows itself when it is given
        // something to say. Blank was not enough: a hidden label takes no room,
        // an empty one still takes its own margin - so the test case editor,
        // which never writes any of them, carried thirty pixels of nothing in
        // front of the page size and put that field somewhere the run editor
        // never put it. The two bars now differ by exactly the run's figures.
        runStatusLabel.setVisible(false);
        verdictsLabel.setVisible(false);
        executionTimeLabel.setVisible(false);

        pageSizeField.setHorizontalAlignment(SwingConstants.CENTER);
        // Its own border and nothing else: on a text field the border is the frame
        // the tester sees, so replacing it to buy a margin erases the field. The
        // margin belongs to the row, below.
        pageSizeField.setToolTipText("Test cases per page");

        navigationRow = centeredRow(firstButton, prevButton, currentPageLabel, nextButton, lastButton);
        rightRow = centeredRow(runStatusLabel, verdictsLabel, executionTimeLabel, pageSizeField);

        add(statusLabel);
        add(navigationRow);
        add(rightRow);
    }

    /**
     * Left against the left edge, right against the right edge, and the arrows in
     * the middle of the bar.
     * <p>
     * Laid out here rather than by a layout manager because no standard one can
     * say that. A border layout and a grid bag both center the middle child in
     * whatever room is left over between the two ends - so the arrows sat off to
     * one side by half the difference between the sentence and the figures, and
     * moved every time either of them changed length.
     * <p>
     * What gives way is decided here too, in order: the figures on the right keep
     * their width, because a page field that is hard to hit and counts that are
     * cut in half are worse than a shorter sentence. The arrows are placed in the
     * middle and then pushed left only if they would otherwise run into the
     * figures. The sentence takes whatever is left and ends in an ellipsis, which
     * a label does for itself once it is drawn narrower than its text.
     */
    @Override
    public void doLayout() {
        final @NotNull Insets insets = getInsets();
        final int top = insets.top;
        final int height = getHeight() - insets.top - insets.bottom;
        final int left = insets.left;
        final int right = getWidth() - insets.right;

        final int rightWidth = Math.min(rightRow.getPreferredSize().width, Math.max(0, right - left));
        rightRow.setBounds(right - rightWidth, top, rightWidth, height);

        final int navWidth = navigationRow.getPreferredSize().width;
        final int centered = left + ((right - left) - navWidth) / 2;
        final int navX = Math.max(left, Math.min(centered, right - rightWidth - navWidth));
        navigationRow.setBounds(navX, top, navWidth, height);

        statusLabel.setBounds(left, top, Math.max(0, navX - left), height);
    }

    /**
     * The toolbar declares the height and this matches it, so the two strips
     * framing an editor are the same - or taller when what is in them needs it,
     * which is the part that was missing.
     */
    @Override
    public @NotNull Dimension getPreferredSize() {
        int width = 0;
        int height = 0;

        for (final Component region : getComponents()) {
            final @NotNull Dimension size = region.getPreferredSize();
            width += size.width;
            height = Math.max(height, size.height);
        }

        final @NotNull Insets insets = getInsets();

        return new Dimension(width + insets.left + insets.right, AbstractToolbarPanel.barHeight(height + insets.top + insets.bottom));
    }

    /**
     * A row of components sitting in the middle of the bar's height.
     * <p>
     * {@link GridBagLayout} rather than a {@link FlowLayout}: a flow lays its one
     * row out from the top of whatever space it is given, and both of these rows
     * are stretched to the full height of the bar - so every arrow and every label
     * sat against the top edge with the rest of the bar empty beneath it. Grid bag
     * centers on both axes by default, which is the whole reason it is here.
     */
    private static @NotNull JBPanel<?> centeredRow(final @NotNull JComponent... items) {
        final @NotNull JBPanel<?> row = new JBPanel<>(new GridBagLayout());
        row.setOpaque(false);
        // Transparent, and still asked for its color: an arrow paints the ground
        // under its hover pill from whatever its parent reports, and a panel that
        // never had one set reports the plain panel background, not this bar's.
        row.setBackground(JBUI.CurrentTheme.EditorTabs.background());

        final @NotNull GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        // On the left of each item only. A gap on both sides would leave one
        // after the last item too, which is the right-hand edge - so the bar
        // would sit further from its right edge than its left by exactly that,
        // however carefully EDGE was set.
        gbc.insets = JBUI.insets(0, 4, 0, 0);

        for (final JComponent item : items) row.add(item, gbc);

        return row;
    }

    /**
     * Already formatted by the caller: the blank for "nothing measured" is
     * decided by {@code Display.formatDuration}, not here.
     */
    public void showExecutionTime(final @NotNull String formatted) {
        executionTimeLabel.setText(formatted);
        executionTimeLabel.setVisible(!formatted.isEmpty());
    }

    /**
     * How many cases carry each verdict, already phrased by
     * {@code ResultAnalysis.headline}. Blank for a run with nothing recorded,
     * which is the rule the execution time beside it already follows.
     */
    public void showVerdicts(final @NotNull String formatted) {
        verdictsLabel.setText(formatted);
        verdictsLabel.setVisible(!formatted.isEmpty());
    }

    /**
     * Where the run stands, said in the bar rather than only in a tooltip.
     * <p>
     * A completed or closed run refuses verdicts and result edits, and the one
     * place that was written was the Start button's tooltip - so a tester whose
     * keystroke did nothing had to hover the button that did nothing to find out
     * why. The status is the fact behind that refusal, so it belongs where they
     * are already looking.
     * <p>
     * The status is handed in rather than its label, because the constant carries
     * its own presentation: the icon here is the one the project tree draws for
     * the same run, so the two cannot come to show different states.
     */
    public void showRunStatus(final @NotNull TestRunStatus status) {
        runStatusLabel.setIcon(status.getIcon());
        runStatusLabel.setText(status.getLabel());
        runStatusLabel.setVisible(true);
    }

    /**
     * The page indicator and the arrows. Not the left label.
     * <p>
     * It used to write that too, as "0 of N" against the filtered count, while
     * the selection update writes it against the unfiltered total - and this one
     * always landed last, because both editors called it after restoring the
     * selection. So after any filter change, reload or status change the tester
     * saw a highlighted row above a status bar reading "0 of 12 test cases", and
     * clicking that same row changed it to "3 of 120": a different number and a
     * different denominator for one screen.
     * <p>
     * That text is the no-selection branch of {@code updateSelectionState},
     * which owns the label. The editors call that after restoring the selection
     * now, so nothing here has to guess what is selected.
     */
    public void updatePaginationState(final int currentPage, final int totalPages) {
        currentPageLabel.setText(currentPage + " of " + Math.max(1, totalPages));

        firstButton.setEnabled(currentPage > 1);
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
        lastButton.setEnabled(currentPage < totalPages);
    }

    /**
     * The one writer of the left label.
     * <p>
     * The row's position in the whole list is handed in rather than worked out
     * here: this was a fourth copy of {@code (page - 1) * pageSize + row}, a
     * conversion the editor interface owns because the card, the hover hit-test
     * and the transfer handler all need the same answer.
     * <p>
     * Both counts are handed in because they are not the same number when a
     * filter or a search is on. This read "3 of 120 test cases" over twelve
     * visible rows - the position counted in the narrowed list, the total counted
     * in the whole one - so the tester could not tell whether the missing hundred
     * and eight were on later pages or filtered out. When nothing is narrowed the
     * two agree and the sentence is the one it always was.
     * <p>
     * One case is a test case. The count and its noun were a format string with
     * a fixed plural in it, so a set with one case in it, and every set filtered
     * down to one, read "1 of 1 test cases".
     */
    public void updateSelectionState(final int @NotNull [] selectedIndices, final int firstSelectedPosition, final int shownCount, final int totalCount) {
        final int selectedCount = selectedIndices.length;
        final @NotNull String cases = shownCount + (shownCount == 1 ? " test case" : " test cases");
        final @NotNull String of = cases + (shownCount == totalCount ? "" : " (filtered from " + totalCount + ")");

        if (selectedCount > 1) {
            statusLabel.setText(String.format(Locale.ENGLISH, "%d selected of %s", selectedCount, of));

        } else if (selectedCount == 1) {
            statusLabel.setText(String.format(Locale.ENGLISH, "%d of %s", firstSelectedPosition + 1, of));

        } else {
            statusLabel.setText(String.format(Locale.ENGLISH, "0 of %s", of));
        }
    }
}
