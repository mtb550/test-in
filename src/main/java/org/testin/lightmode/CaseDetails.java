package org.testin.lightmode;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.Shared;
import org.testin.model.Group;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Display;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
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
     * The label column, in the design's own figure. Narrow because the window is
     * narrow and the label is one word set small - and it grows with the zoom,
     * because a label set larger in a column that did not would wrap.
     */
    private static final int LABEL_WIDTH = 92;

    private static final int GAP = 10;

    private final @NotNull Project p;

    /**
     * The case on screen, kept so the rows can be rebuilt without being handed
     * it again - which is what a zoom is.
     */
    private @NotNull Optional<TestCaseDto> shown = Optional.empty();

    private float zoom = 1.0f;

    CaseDetails(final @NotNull Project p) {
        super(new GridBagLayout());
        this.p = p;
        setOpaque(false);
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
     * Scales the field labels and their values, and nothing else in the window.
     * <p>
     * The rows are rebuilt rather than walked and re-fonted: they are rebuilt on
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

        int row = 0;
        row = addRow(TestEditorAttributes.STEPS.getName(), Display.numberedSteps(tc.getSteps()), row);

        // Verbatim, and not through Display: test data is credentials, a query, a
        // payload - values that are used rather than read, so a character this
        // window decides to drop is a value that no longer works. The same rule
        // the details panel states, for the same reason.
        row = addRow(TestEditorAttributes.TEST_DATA.getName(), tc.getTestData(), row);
        row = addRow(TestEditorAttributes.PRE_CONDITIONS.getName(), Display.format(tc.getPreConditions()), row);

        addTags(tc, row);
    }

    private int addRow(final @NotNull String name, final @NotNull String value, final int row) {
        if (value.isBlank()) return row;

        return addRow(name, prose(value), row);
    }

    private int addRow(final @NotNull String name, final @NotNull JComponent value, final int row) {
        final @NotNull GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
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

        return row + 1;
    }

    /**
     * The case's priority and its groups, drawn as the badges the cards and the
     * details panel already draw. No run status among them: this window is open
     * because the tester is running the case by hand, so a badge saying whether
     * automation reached it is answering a question nobody in front of it asked.
     */
    private void addTags(final @NotNull TestCaseDto tc, final int row) {
        final @NotNull List<Shared.Badge> badges = new ArrayList<>();
        Shared.addPriorityBadge(badges, tc);

        for (final Group group : tc.getGroup()) {
            badges.add(Shared.createGroupBadge(group));
        }

        if (badges.isEmpty()) return;

        final @NotNull JBPanel<?> chips = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(5), 0));
        chips.setOpaque(false);
        Shared.showBadges(chips, badges);

        addRow(TestEditorAttributes.GROUP.getName(), chips, row);
    }

    private @NotNull JBLabel label(final @NotNull String text) {
        // ROOT, not the tester's locale: these are four fixed English field
        // names, and a Turkish machine would render "Conditions" with a dotted
        // capital I.
        final @NotNull JBLabel label = new JBLabel(text.toUpperCase(Locale.ROOT));
        label.setFont(scaled(CaseFont.label()));
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        final @NotNull Dimension size = new Dimension(Math.round(JBUI.scale(LABEL_WIDTH) * zoom), label.getPreferredSize().height);
        label.setPreferredSize(size);
        label.setMinimumSize(size);

        return label;
    }

    private @NotNull JTextArea prose(final @NotNull String text) {
        final @NotNull JTextArea area = Prose.of(scaled(CaseFont.body()), JBUI.CurrentTheme.Label.foreground());
        area.setText(text);

        return area;
    }

    private @NotNull Font scaled(final @NotNull Font base) {
        return base.deriveFont(base.getSize2D() * zoom);
    }
}
