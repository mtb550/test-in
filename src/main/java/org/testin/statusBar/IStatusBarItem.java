package org.testin.statusBar;

import org.jetbrains.annotations.NotNull;

public interface IStatusBarItem {
    @NotNull String getShortcutText();

    @NotNull String getName();
}