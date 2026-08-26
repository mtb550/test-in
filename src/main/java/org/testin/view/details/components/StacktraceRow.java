package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * The first few lines of a failure's stacktrace, and a link to the rest.
 * <p>
 * Its own row type rather than a {@link RunAttributeRow} that checks which
 * attribute it is holding: every other run value is a word or a sentence, and
 * this one is fifty lines of framework plumbing around the two that name the
 * tester's own code. Given the whole panel it would be the whole panel.
 * <p>
 * Three lines because that is what the top of a stacktrace is worth: the frame
 * that threw, and enough beneath it to recognize where. Everything else is read
 * in {@link ErrorDetailsDialog}, once, by a tester who has decided they need it.
 */
public final class StacktraceRow extends BaseDetails {

    /**
     * How much of the trace the panel shows before handing over to the dialog.
     */
    private static final int LINES_SHOWN = 3;

    private static final int LINK_MARGIN_TOP = 6;

    private final @NotNull TestRunItems item;

    public StacktraceRow(final @NotNull TestRunItems item) {
        this.item = item;
    }

    /**
     * A case with nothing to explain draws no row - the same rule every other
     * run row follows, and the reason a passing case shows none of them.
     */
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull String stacktrace = item.getStacktrace();
        if (stacktrace.isBlank()) return currentRow;

        final @NotNull List<String> lines = stacktrace.lines().toList();

        final @NotNull JBPanel<?> container = new JBPanel<>();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);

        container.add(preview(lines));

        if (lines.size() > LINES_SHOWN) {
            container.add(showAllLink(p, dto, stacktrace, lines.size()));
        }

        return addRow(panel, gbc, RunEditorAttributes.STACKTRACE.getName(), container, currentRow);
    }

    /**
     * Monospaced and not wrapped. A stacktrace read in a proportional font
     * loses the indentation that makes it scannable, and wrapping one long
     * frame across two lines would spend a third of the preview on it.
     */
    private @NotNull JTextArea preview(final @NotNull List<String> lines) {
        final @NotNull JTextArea area = new JTextArea(String.join("\n", lines.subList(0, Math.min(LINES_SHOWN, lines.size()))));
        area.setFont(JBFont.create(new Font(Font.MONOSPACED, Font.PLAIN, (int) getValueFontSize())));
        area.setOpaque(false);
        area.setEditable(false);
        area.setBorder(null);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    /**
     * Named with the count rather than "more", so a tester knows before
     * clicking whether the rest is two lines or eighty.
     */
    private @NotNull ActionLink showAllLink(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull String stacktrace, final int total) {
        // Typed rather than inline: ActionLink also takes a Kotlin function of
        // the same shape, and an untyped lambda matches both.
        final @NotNull ActionListener onClick = event ->
                new ErrorDetailsDialog(p, dto.getDescription(), item.getActualResult(), stacktrace).show();

        final @NotNull ActionLink link = new ActionLink("Show all " + total + " lines", onClick);

        link.setFont(JBFont.label().deriveFont(getValueFontSize()));
        link.setBorder(JBUI.Borders.emptyTop(LINK_MARGIN_TOP));
        link.setAlignmentX(Component.LEFT_ALIGNMENT);
        return link;
    }
}
