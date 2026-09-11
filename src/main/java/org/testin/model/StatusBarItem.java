package org.testin.model;

import org.jetbrains.annotations.NotNull;

public interface StatusBarItem {
    @NotNull String getShortcutText();

    @NotNull String getName();
}