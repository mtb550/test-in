package org.testin.editor;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Locale;

/**
 * Shared colors for list and grid selection states.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditorColors {
    public static final @NotNull Color SELECTION_BACKGROUND = new JBColor(
            new Color(214, 230, 250),
            new Color(37, 55, 76)
    );
    public static final @NotNull Color SELECTION_BORDER = JBColor.blue;

    /**
     * One color for one fact: a filter is on and the tester is not seeing
     * everything.
     * <p>
     * It marks the filter button's count and the status bar's own "(filtered
     * from 120)" in the same breath, because a tester who has stopped noticing
     * one has stopped noticing the other - and a second color for the same fact
     * would say they were two.
     */
    public static final @NotNull Color FILTER_ACTIVE = JBUI.CurrentTheme.Link.Foreground.ENABLED;

    /**
     * That color as markup, for the one label that has to color part of a
     * sentence rather than all of it.
     */
    public static @NotNull String filterActiveHex() {
        return String.format(Locale.ENGLISH, "#%02x%02x%02x",
                FILTER_ACTIVE.getRed(), FILTER_ACTIVE.getGreen(), FILTER_ACTIVE.getBlue());
    }
}
