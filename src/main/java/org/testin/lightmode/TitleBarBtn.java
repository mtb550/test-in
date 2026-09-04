package org.testin.lightmode;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractIconButton;

import javax.swing.Icon;

/**
 * A button in light mode's own title bar, drawn the way every other button in
 * this plugin is drawn.
 * <p>
 * One class for the pin and the close, for the reason {@code PageBtn} gives:
 * the toolbar's one-class-per-button exists so {@code getToolbarItem} can find
 * them by type, and nothing looks these two up.
 */
class TitleBarBtn extends AbstractIconButton {

    TitleBarBtn(final @NotNull String tooltip, final @NotNull Icon icon) {
        super(tooltip, icon);
    }
}
