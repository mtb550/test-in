package org.testin.ui.framework;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;

/**
 * A keystroke drawn as a key rather than printed as text.
 * <p>
 * The heavier bottom edge is the whole trick: one pixel on three sides and two
 * underneath reads as a keycap, where an even border reads as a box around some
 * text.
 * <p>
 * One owner, because two surfaces show keys and they must show them alike -
 * every framework dialog's status bar, and light mode's verdict buttons, where
 * P, F and B are the primary way a tester works (#13). Drawn in two places, the
 * next adjustment to the border or the font would reach one of them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Keycap {

    /**
     * A blank keystroke is left as bare text. Some status bar items carry no key
     * of their own - a create-dialog field with no letter - and an empty keycap
     * is a box drawn around nothing.
     */
    public static @NotNull JBLabel of(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setForeground(JBUI.CurrentTheme.Label.foreground());
        label.setFont(JBUI.Fonts.smallFont().asBold());

        if (text.isBlank()) return label;

        label.setOpaque(true);
        label.setBackground(UIUtil.getTextFieldBackground());
        label.setBorder(BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBColor.border(), 1, 1, 2, 1),
                JBUI.Borders.empty(0, 5)));

        return label;
    }
}
