package org.testin.lightmode;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Badges;
import org.testin.ui.framework.Prose;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.util.Display;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.List;
import java.util.Optional;

/**
 * What the case says beyond its description: the steps, the data to use, what
 * has to be true first, and how it is tagged (#13).
 * <p>
 * <b>Not the details panel's rows.</b> Reusing them was the first plan and the
 * measurement refused it: {@code LabelValueRow} pins its label column to 255
 * pixels, which with its own insets is 287 of this window's 420 before the
 * value is given any - and it sets that as a minimum, so the window could not
 * be narrow at all. The design asks for 92. What is shared instead is
 * everything that is knowledge rather than layout: the field names come from
 * {@link TestEditorAttributes} - the bare name, since the panel's trailing
 * colon reads wrong under a small-caps label - the step numbering from
 * {@link Display}, and the tags are the plugin's own badges.
 * <p>
 * A blank field is not drawn. Most cases fill in two of these four, and a row
 * with a dash after it is a line read on every case to learn nothing.
 */
class CaseDetails extends JBPanel<CaseDetails> {

    /**
     * What the last row is called. Not GROUP: it draws the priority badge as
     * well, so a High-priority case in no group read "GROUP: High".
     */
    private static final @NotNull String TAGS = Bundle.message("light.tags");

    /**
     * The label column, in the design's own figure. Narrow because the window is
     * narrow and the label is one word set small - and it grows with the zoom,
     * because a label set larger in a column that did not would wrap.
     */
    private static final int LABEL_WIDTH = 92;

    private static final int GAP = 10;

    /**
     * The case on screen, kept so the rows can be rebuilt without being handed
     * it again - which is what a zoom is.
     */
    private @NotNull Optional<TestCaseDto> shown = Optional.empty();

    /** Only so the rows can ask an attribute how it is displayed. */
    private final @NotNull Project p;

    private float zoom = 1.0f;

    /**
     * Whether the window ran out of screen before the case ran out of text.
     * <p>
     * The window clamps to the display and says so rather than growing past the
     * bottom of it, which is where the verdict buttons would have gone (#66,
     * finding 54). Held here because it is drawn here, and set by the window,
     * which is the only thing that knows how much screen there is.
     */
    private boolean cutOff = false;

    CaseDetails(final @NotNull Project p) {
        super(new GridBagLayout());
        setOpaque(false);

        this.p = p;
    }

    /**
     * Redraws for this case. Every row is rebuilt rather than updated: there are
     * four of them, they change only when the case does, and a panel that
     * rebuilds cannot leave the previous case's steps under the new one's tags.
     */
    void show(final @NotNull TestCaseDto tc) {
        shown = Optional.of(tc);

        render();
    }

    /**
     * UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-217.
     * <p>
     * Told by the window, which is the only thing that knows how much screen
     * there is. Redraws only when the answer changed, because this is asked on
     * every refresh - a clock tick, a verdict, a resize.
     */
    void setCutOff(final boolean truncated) {
        if (cutOff == truncated) return;

        cutOff = truncated;
        render();
    }

    /**
     * Scales the field labels and their values, and nothing else in the window.
     * <p>
     * The rows are rebuilt rather than walked and re-sized: they are rebuilt on
     * every case anyway, and a walk would have to know which of the components
     * under here are text the tester reads and which are the badges, which are
     * sized by the plugin rather than by this window.
     */
    void setZoom(final float zoom) {
        if (this.zoom == zoom) return;

        this.zoom = zoom;
        render();
    }

    private void render() {
        removeAll();

        shown.ifPresent(this::rows);
    }

    private void rows(final @NotNull TestCaseDto tc) {
        addRow(TestEditorAttributes.STEPS.getName(), Display.numberedSteps(tc.getSteps()));

        // Verbatim, and not through Display: test data is credentials, a query, a
        // payload - values that are used rather than read, so a character this
        // window decides to drop is a value that no longer works. The same rule
        // the details panel states, for the same reason.
        addRow(TestEditorAttributes.TEST_DATA.getName(), tc.getTestData());
        addRow(TestEditorAttributes.PRE_CONDITIONS.getName(), TestEditorAttributes.PRE_CONDITIONS.displayValue(tc));

        addTags(tc);

        // Last, under everything it is about. A tester who cannot see the rest
        // of the case has one thing to do about it, and the row says what.
        if (cutOff) addCutOffNotice();
    }

    /**
     * UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-217.
     * <p>
     * The case is longer than the screen, and the window stopped rather than
     * putting its verdict buttons past the bottom of the display.
     * <p>
     * Said rather than left to be discovered: a window that simply stopped would
     * have the tester believe they had read the whole case, which is the one
     * thing this window exists to be trusted about.
     */
    private void addCutOffNotice() {
        final @NotNull JBLabel notice = new JBLabel(Bundle.message("light.cut.off"));
        notice.setFont(CaseFont.zoomed(CaseFont.label(), zoom));
        notice.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        addRow("", notice);
    }

    private void addRow(final @NotNull String name, final @NotNull String value) {
        if (value.isBlank()) return;

        addRow(name, prose(value));
    }

    private void addRow(final @NotNull String name, final @NotNull JComponent value) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();

        // RELATIVE, so the layout counts the rows rather than this class
        // threading a number through four methods to tell it what it knows.
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, GAP, 0);

        gbc.gridx = 0;
        gbc.weightx = 0;
        add(label(name), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = JBUI.insets(0, GAP, GAP, 0);
        add(value, gbc);
    }

    /**
     * The case's priority and its groups, drawn as the badges the cards and the
     * details panel already draw. No run status among them: this window is open
     * because the tester is running the case by hand, so a badge saying whether
     * automation reached it is answering a question nobody in front of it asked.
     */
    private void addTags(final @NotNull TestCaseDto tc) {
        final @NotNull List<Badges.Badge> badges = Badges.caseBadges(tc);

        if (badges.isEmpty()) return;

        final @NotNull JBPanel<?> chips = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(5), 0));
        chips.setOpaque(false);
        Badges.showBadges(chips, badges);

        // Labeled for what the row holds rather than for the groups alone: a
        // High-priority case in no group drew a row reading GROUP: High.
        addRow(TAGS, chips);
    }

    private @NotNull JBLabel label(final @NotNull String text) {
        // ROOT, not the tester's locale: these are four fixed English field
        // names, and a Turkish machine would render "Conditions" with a dotted
        // capital I.
        final @NotNull JBLabel label = new JBLabel(text.toUpperCase(Locale.ROOT));
        label.setFont(CaseFont.zoomed(CaseFont.label(), zoom));
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        final @NotNull Dimension size = new Dimension(Math.round(JBUI.scale(LABEL_WIDTH) * zoom), label.getPreferredSize().height);
        label.setPreferredSize(size);
        label.setMinimumSize(size);

        return label;
    }

    private @NotNull JTextArea prose(final @NotNull String text) {
        final @NotNull JTextArea area = Prose.of(CaseFont.zoomed(CaseFont.body(), zoom), JBUI.CurrentTheme.Label.foreground());
        area.setText(text);

        return area;
    }

}
