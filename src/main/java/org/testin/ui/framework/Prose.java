package org.testin.ui.framework;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;

/**
 * A paragraph the tester reads: wrapped, and furniture rather than a field.
 * <p>
 * A label prints its text on one line and would let a sentence set the width of
 * the window; a text area wraps, which is the whole reason this is one. The five
 * settings underneath are what turn a text area back into something that reads
 * as text - no background of its own, no caret, no border, no tab stop.
 * <p>
 * <b>The read-only half of a pair.</b> {@link TextArea} is the box a tester
 * types into; this is the paragraph they read. Both wrap, and that is the whole
 * of what they share, which is why they are two classes rather than one with a
 * flag.
 * <p>
 * <b>One owner, so the look cannot drift.</b> It was written for light mode's
 * two paragraphs. The same lines were spelled out again in the test case card,
 * the details panel's rows, its steps and its title - five copies of one idea,
 * each free to be corrected without the others. They read from here now (#178).
 * <p>
 * Three of those copies left the tab stop in - only the card and light mode
 * had set it - so a paragraph nobody can type into was still in the focus
 * order. Gathering them fixed that everywhere at once, which is the argument
 * for gathering them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Prose {

    /**
     * A paragraph holding this text. Font and color are the caller's, because
     * they are the only things that differ between the places prose is drawn.
     */
    public static @NotNull JTextArea of(final @NotNull String text) {
        final @NotNull JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setBorder(null);

        return area;
    }

    /**
     * Empty, for a paragraph built once and filled again on every case - which
     * is what a window showing one case at a time does with both of its.
     */
    public static @NotNull JTextArea of(final @NotNull Font font, final @NotNull Color color) {
        final @NotNull JTextArea area = of("");
        area.setFont(font);
        area.setForeground(color);

        return area;
    }
}
