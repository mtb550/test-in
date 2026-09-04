package org.testin.lightmode;

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
 * One owner because light mode draws prose in two places, the case and its
 * details, and they must not drift apart. The same five lines are spelled out in
 * nine other files across the plugin; those are not this patch's to gather up,
 * but a tenth and eleventh copy in one package would have been.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Prose {

    static @NotNull JTextArea of(final @NotNull Font font, final @NotNull Color color) {
        final @NotNull JTextArea area = new JTextArea();
        area.setFont(font);
        area.setForeground(color);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setBorder(null);

        return area;
    }
}
