package org.testin.statusbar;

import org.jetbrains.annotations.NotNull;

/**
 * Plain name + shortcut pair for the dialog status bars.
 */
record ShortcutItem(@NotNull String name, @NotNull String shortcutText) implements StatusBarItem {

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcutText;
    }
}
