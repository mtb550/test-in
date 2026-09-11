package org.testin.editor;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

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
}
