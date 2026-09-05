package org.testin.ui.framework;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * The two backgrounds a list alternates between, so a row can be told from the
 * one under it without a line drawn between them.
 * <p>
 * <b>One owner, because there were two and they agreed only by luck.</b> The
 * grid held them as a pair of constants and the card list wrote the same two
 * {@code JBColor}s inline at its own call site - the same four greys, in two
 * files, either free to be adjusted without the other. A tester switching
 * between the grid and the list is looking at one striping, and it has to stay
 * one.
 * <p>
 * A {@link JBColor} rather than a color read from the theme once: it resolves
 * per paint, so a theme switched while the IDE is running is picked up without
 * anything being rebuilt.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RowStripe {

    private static final @NotNull Color EVEN = new JBColor(Gray._245, Gray._60);
    private static final @NotNull Color ODD = new JBColor(Gray._230, Gray._45);

    /**
     * The background of the row at this index, counting from zero.
     */
    public static @NotNull Color of(final int index) {
        return index % 2 == 0 ? EVEN : ODD;
    }

    /**
     * The odd row's color on its own, for a surface that is not in a list at
     * all.
     * <p>
     * Light mode's failure fields take it. A window painted as frame decoration
     * has no panel color behind its inputs to set them apart, and this is a grey
     * the tester is already looking at in the grid and the card list - so the
     * window borrows one rather than introducing a fifth.
     */
    public static @NotNull Color odd() {
        return ODD;
    }
}
