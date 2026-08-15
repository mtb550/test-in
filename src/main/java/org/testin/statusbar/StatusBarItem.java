package org.testin.statusbar;

import org.jetbrains.annotations.NotNull;

public interface StatusBarItem {
    @NotNull String getShortcutText();

    @NotNull String getName();
}