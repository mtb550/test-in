package org.testin.statusbar;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Something that can be a row on a shortcut menu: it has a name, an icon, a
 * shortcut to print, and knows how to bind that shortcut itself.
 * <p>
 * Every implementor is an enum, and every one of them already answered all four
 * questions - {@code ShortcutMenuPopup} was simply handed the four answers as
 * separate method references, five functional arguments on an eight-argument
 * constructor that nobody could read at a glance (#175, C12).
 * <p>
 * Naming the thing rather than passing its parts also puts the binding where the
 * constant is. A menu row's shortcut belongs to the row, not to the popup that
 * happens to be showing it, which is why {@code bindShortcut} is declared here
 * and why the {@code ItemShortcutBinder} that used to carry it across is gone.
 */
public interface MenuItem extends StatusBarItem {

    @NotNull Icon getIcon();

    /**
     * Makes this item's own key select it, on whatever component is showing the
     * menu. A constant with no shortcut binds nothing.
     */
    void bindShortcut(@NotNull JComponent component, @NotNull Runnable onTrigger);
}
